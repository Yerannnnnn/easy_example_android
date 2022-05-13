package im.zego.easyexample.android.ui;import android.Manifest;import android.content.DialogInterface;import android.content.DialogInterface.OnDismissListener;import android.content.Intent;import android.os.Bundle;import android.util.Log;import android.view.View;import android.view.View.OnClickListener;import android.widget.Toast;import androidx.annotation.NonNull;import androidx.appcompat.app.AppCompatActivity;import com.blankj.utilcode.util.Utils;import com.google.android.gms.tasks.OnCompleteListener;import com.google.android.gms.tasks.Task;import com.google.firebase.messaging.FirebaseMessaging;import com.permissionx.guolindev.PermissionX;import im.zego.easyexample.android.HttpClient;import im.zego.easyexample.android.HttpClient.HttpResult;import im.zego.easyexample.android.cloudmessage.CloudMessage;import im.zego.easyexample.android.cloudmessage.CloudMessageManager;import im.zego.easyexample.android.cloudmessage.CloudMessageManager.CloudMessageListener;import im.zego.easyexample.android.cloudmessage.NotificationHelper;import im.zego.easyexample.android.databinding.ActivityLoginBinding;import im.zego.easyexample.android.express.AppCenter;import im.zego.example.express.ExpressManager;import im.zego.example.express.ZegoMediaOptions;import im.zego.example.ringtone.RingtoneManager;import im.zego.example.ui.ReceiveCallDialog;import im.zego.example.ui.ReceiveCallView;import im.zego.example.ui.ReceiveCallView.OnReceiveCallViewClickedListener;import im.zego.example.ui.ReceiveCallView.ReceiveCallData;import im.zego.zegoexpress.callback.IZegoRoomLoginCallback;import im.zego.zegoexpress.entity.ZegoUser;import org.json.JSONObject;public class LoginActivity extends AppCompatActivity {    private ActivityLoginBinding binding;    private static final String TAG = "LoginActivity";    String selfID = "876543";    String selfName = "大灰狼2";    String selfIcon =        "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fimg.allyfurn.com%2Fwp-content%2Fuploads%2F2020%2F05%2F0H30J202-0.png"            + "&refer=http%3A%2F%2Fimg.allyfurn.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=auto?sec=1654936387&t=5714f6533c6fff1bcc850c561cd432c9";    private boolean isOnResumed;    @Override    public void onCreate(Bundle savedInstanceState) {        super.onCreate(savedInstanceState);        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");        binding = ActivityLoginBinding.inflate(getLayoutInflater());        setContentView(binding.getRoot());        Utils.init(getApplication());        binding.selfUserId.setEnabled(false);        binding.selfUserId.setText(selfID);        binding.selfRegister.setOnClickListener(new OnClickListener() {            @Override            public void onClick(View v) {                getAndRegisterToken(binding.selfUserId.getText().toString());            }        });        binding.callUser.setOnClickListener(new OnClickListener() {            @Override            public void onClick(View v) {                PermissionX.init(LoginActivity.this)                    .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)                    .request((allGranted, grantedList, deniedList) -> {                        if (allGranted) {                            CloudMessage cloudMessage = new CloudMessage();                            cloudMessage.targetUserID = binding.targetUserId.getText().toString();                            cloudMessage.roomID = "7634";                            cloudMessage.callType = "Video";                            cloudMessage.callerUserID = selfID;                            cloudMessage.callerUserName = selfName;                            cloudMessage.callerIconUrl = selfIcon;                            HttpClient.getInstance().callUserByCloudMessage(cloudMessage, new HttpResult() {                                @Override                                public void onResult(int errorCode, String result) {                                    if (errorCode == 0) {                                        binding.getRoot().post(new Runnable() {                                            @Override                                            public void run() {                                                joinRoom(cloudMessage.roomID, cloudMessage.callerUserID,                                                    cloudMessage.callerUserName);                                            }                                        });                                    } else {                                        Toast.makeText(getApplication(), "callUserByCloudMessage failed:" + result,                                            Toast.LENGTH_LONG).show();                                    }                                }                            });                        }                    });            }        });        ExpressManager.getInstance().createEngine(getApplication(), AppCenter.appID);        PermissionX.init(this)            .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)            .request((allGranted, grantedList, deniedList) -> {            });        CloudMessageManager.getInstance().setListener(new CloudMessageListener() {            @Override            public void onMessageReceived(CloudMessage cloudMessage) {                if (isOnResumed) {                    onCloudMessageReceived(cloudMessage);                }            }        });        CloudMessage cloudMessage = CloudMessageManager.getInstance().getCloudMessage();        if (cloudMessage != null) {            onCloudMessageReceived(cloudMessage);            CloudMessageManager.getInstance().clearCloudMessage();        }    }    private void getAndRegisterToken(String userID) {        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {            @Override            public void onComplete(@NonNull Task<String> task) {                if (!task.isSuccessful()) {                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());                    return;                }                // Get new FCM registration token                String token = task.getResult();                // Log and toast                Log.w(TAG, "Fetching FCM registration token " + token);                Toast.makeText(LoginActivity.this, "token: " + token, Toast.LENGTH_SHORT).show();                HttpClient.getInstance().registerFCMToken(userID, token, new HttpResult() {                    @Override                    public void onResult(int errorCode, String result) {                        if (errorCode == 0) {                            Toast.makeText(LoginActivity.this, "register "                                + "fcmToken successed", Toast.LENGTH_SHORT).show();                        } else {                            Toast.makeText(getApplication(), "register fcmToken failed:" + errorCode,                                Toast.LENGTH_LONG).show();                        }                    }                });            }        });    }    @Override    protected void onResume() {        super.onResume();        NotificationHelper.cancelNotification(this);        isOnResumed = true;    }    @Override    protected void onPause() {        super.onPause();        isOnResumed = false;    }    private void joinRoom(String roomID, String userID, String username) {        if (!checkAppID()) {            Toast.makeText(this, "please add appID and "                + "serverSecret in AppCenter.java", Toast.LENGTH_SHORT).show();            return;        }        binding.loading.setVisibility(View.VISIBLE);        ZegoUser user = new ZegoUser(userID, username);        HttpClient.getInstance().getRTCToken(selfID, new HttpResult() {            @Override            public void onResult(int errorCode, String token) {                if (errorCode == 0) {                    int mediaOptions = ZegoMediaOptions.autoPlayAudio | ZegoMediaOptions.autoPlayVideo |                        ZegoMediaOptions.publishLocalAudio | ZegoMediaOptions.publishLocalVideo;                    ExpressManager.getInstance()                        .joinRoom(roomID, user, token, mediaOptions, new IZegoRoomLoginCallback() {                            @Override                            public void onRoomLoginResult(int errorCode, JSONObject jsonObject) {                                binding.loading.setVisibility(View.GONE);                                if (errorCode == 0) {                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));                                } else {                                    Toast.makeText(getApplication(), "join room failed,errorCode :" + errorCode,                                        Toast.LENGTH_LONG).show();                                }                            }                        });                } else {                    Toast.makeText(getApplication(), "join room get token failed:" + errorCode,                        Toast.LENGTH_LONG).show();                }            }        });    }    private boolean checkAppID() {        return AppCenter.appID != 0L;    }    public void onCloudMessageReceived(CloudMessage cloudMessage) {        RingtoneManager.playRingTone(this);        ReceiveCallData callData = new ReceiveCallData();        callData.callUserName = cloudMessage.callerUserName;        callData.callUserID = cloudMessage.callerUserID;        callData.callUserIcon = cloudMessage.callerIconUrl;        callData.callType = "Video".equals(cloudMessage.callType)            ? ReceiveCallData.Video : ReceiveCallData.Voice;        ReceiveCallView view = new ReceiveCallView(this);        view.setReceiveCallData(callData);        ReceiveCallDialog dialog = new ReceiveCallDialog(this, view);        view.setListener(new OnReceiveCallViewClickedListener() {            @Override            public void onAcceptAudioClicked() {                dialog.dismiss();                joinRoom(cloudMessage.roomID, selfID, selfName);            }            @Override            public void onAcceptVideoClicked() {                dialog.dismiss();                joinRoom(cloudMessage.roomID, selfID, selfName);            }            @Override            public void onDeclineClicked() {                dialog.dismiss();            }            @Override            public void onWindowClicked() {            }        });        if (!dialog.isShowing()) {            dialog.show();        }        dialog.setOnDismissListener(new OnDismissListener() {            @Override            public void onDismiss(DialogInterface dialog) {                RingtoneManager.stopRingTone();            }        });    }    @Override    protected void onDestroy() {        super.onDestroy();        RingtoneManager.stopRingTone();    }}