package com.ajay.backgroundplayer;

 import android.content.ActivityNotFoundException;
 import android.content.ComponentName;
 import android.content.Context;
import android.content.Intent;
 import android.net.Uri;
 import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
 import android.view.View;
 import android.widget.Button;

 import com.google.android.exoplayer2.util.Util;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context=this;
        final Intent intent=new Intent(this,AudioPlayerService.class);
        Intent i = getIntent();
        String action = i.getAction();
        String type = i.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                try{
                    stopService(intent);
                }catch (Exception e){

                }
                    String youtubeLink = i.getStringExtra(Intent.EXTRA_TEXT);
                    intent.putExtra("url", youtubeLink);
                    Util.startForegroundService(context, intent);
                    this.moveTaskToBack(true);
            }
        }
        Button min=(Button)findViewById(R.id.button);
        min.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent appIntent = new Intent();
                appIntent.setComponent(new ComponentName("com.google.android.youtube","com.google.android.youtube.PlayerActivity"));

                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://www.youtube.com/"));
                try {
                    context.startActivity(appIntent);
                } catch (ActivityNotFoundException ex) {
                    context.startActivity(webIntent);
                }
            }
        });
    }
}
