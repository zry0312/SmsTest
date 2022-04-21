package com.example.prj10;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.AndroidException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("Range")
public class MessageActivity extends Activity {

    public static final int TAKE_PHOTO = 1;
    public static final int CROP_PHOTO = 2;
    private Button takePhoto;
    private ImageView picture;
    private Uri imageUri;

    ListView messageView;
    ArrayAdapter<String> adapter;
    List<String> messageList = new ArrayList<String>();

    private TextView sender;

    private IntentFilter receiveFilter;
    private MessageReceiver messageReceiver;

    private EditText msgInput;
    private EditText to;
    private Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.msg_layout);

        //显示联系人信息
        sender = (TextView) findViewById(R.id.sender);
        Intent intent = getIntent();
        String displayName = intent.getStringExtra("displayName");
        sender.setText("From:"+displayName);

        messageView = (ListView) findViewById(R.id.message);

        //发送信息操作
        adapter = new ArrayAdapter<String>(MessageActivity.this, android.R.layout.simple_list_item_1,messageList);
        to = (EditText) findViewById(R.id.to);//发送人
        msgInput = (EditText) findViewById(R.id.msg_input);//发送信息
        send = (Button) findViewById(R.id.send);
        messageView.setAdapter(adapter);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = msgInput.getText().toString();
                String number = to.getText().toString();
                if(TextUtils.isEmpty(number)){
                    showToast("请输入手机号");
                    return;
                }
                if(TextUtils.isEmpty(content)){
                    showToast("请输入内容");
                    return;
                }
                ArrayList<String> messages = SmsManager.getDefault().divideMessage(content);
                for(String text : messages){
                    SmsManager.getDefault().sendTextMessage(number, null, text, null, null);
                    msgInput.setText("");//清空
                    messageList.add("me:"+text);
                }
                showToast("发送成功");
            }
            private void showToast(String msg) {
                Toast.makeText(MessageActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });

        receiveFilter = new IntentFilter();
        receiveFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        messageReceiver = new MessageReceiver();
        registerReceiver(messageReceiver,receiveFilter);

        //实现拍照功能
        takePhoto = (Button) findViewById(R.id.take_photo);
        picture = (ImageView) findViewById(R.id.picture);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建File对象，用于存储拍照后的照片
                File outputImage = new File(Environment.getExternalStorageDirectory(),"out_image.jpg");
                try {
                    if (outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                imageUri = Uri.fromFile(outputImage);
                Intent intent1 = new Intent("android.media.action.IMAGE_CAPIURE");
                intent1.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent1,TAKE_PHOTO);//启动相机程序
            }
        });
    }

    //接收信息
    class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter = new ArrayAdapter<String>(MessageActivity.this, android.R.layout.simple_list_item_1,messageList);
            messageView.setAdapter(adapter);
            Bundle bundle = intent.getExtras();
            Object[] pdus = (Object[]) bundle.get("pdus");//提取短信消息
            SmsMessage[] messages = new SmsMessage[pdus.length];

            for(int i = 0; i < messages.length; i++){
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                messageList.add("Other:"+messages[i].getMessageBody());
            }

        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //取消注册
        unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        switch (requestCode){
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK){
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(imageUri,"image/*");
                    intent.putExtra("scale",true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                    startActivityForResult(intent,CROP_PHOTO);//启动裁剪程序
                }
                break;
            case CROP_PHOTO:
                if (resultCode == RESULT_OK){
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);
                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
                break;
            default:
            break;
        }
    }
}
