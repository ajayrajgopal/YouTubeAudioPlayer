package com.ajay.backgroundplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class AudioPlayerService extends Service {
    private SimpleExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    public static final String PLAYBACK_CHANNEL_ID = "playback_channel";
    public static final int PLAYBACK_NOTIFICATION_ID = 1;
    MediaSource mediaSource=null;
    String title="Loading",author="Loading";
    String youtubeLink;
    Bitmap img;
    public static final String MEDIA_SESSION_TAG = "audio_demo";
    public static final String DOWNLOAD_CHANNEL_ID = "download_channel";
    public static final int DOWNLOAD_NOTIFICATION_ID = 2;

    @Override
    public void onCreate(){
        super.onCreate();
    }
    @Override
    public void onDestroy(){
        if (player != null) {
            playerNotificationManager.setPlayer(null);
            player.release();
            player = null;
        }
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        youtubeLink = intent.getStringExtra("url");
        Context context=this;

        HttpGetRequest getRequest = new HttpGetRequest();
        AsyncTaskLoadImage loadImg=new AsyncTaskLoadImage();
        String embedUrl = "https://www.youtube.com/oembed?url="+youtubeLink+"&format=json";

        //Perform the doInBackground method, passing in our url
        try {
            String result = getRequest.execute(embedUrl).get();
            JSONObject jo=new JSONObject(result);
            title=jo.getString("title");
            author=jo.getString("author_name");
            String tb=jo.getString("thumbnail_url");
            img=loadImg.execute(tb).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        playaudio(youtubeLink,context);
        return START_NOT_STICKY;
    }
    public void playaudio(final String youtubeLink, final Context context){
        try {
            YouTubeExtractor ye = new YouTubeExtractor(this) {
                @Override
                public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                    if (ytFiles != null) {
                        int itag = ytFiles.keyAt(0);
                        Log.i("ytFiles",ytFiles.toString());
                        //if(ytFiles.get(itag).getUrl()!=null) {
                            String downloadUrl = ytFiles.get(itag).getUrl();
                            player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
                            DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context,
                                    Util.getUserAgent(context, "AudioDemo"));
                            //ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
                            mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(Uri.parse(downloadUrl));
                            //concatenatingMediaSource.addMediaSource(mediaSource);
                            player.prepare(mediaSource);
                            player.setPlayWhenReady(true);
                            playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(context, PLAYBACK_CHANNEL_ID,
                                    R.string.channel_name, PLAYBACK_NOTIFICATION_ID, new PlayerNotificationManager.MediaDescriptionAdapter() {
                                        @Override
                                        public String getCurrentContentTitle(Player player){
                                            return title;

                                        }

                                        @Nullable
                                        @Override
                                        public PendingIntent createCurrentContentIntent(Player player) {
                                            Intent intent = new Intent(context, MainActivity.class);
                                            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                        }

                                        @Nullable
                                        @Override
                                        public String getCurrentContentText(Player player) {

                                            return author;
                                        }

                                        @Nullable
                                        @Override
                                        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                                            return img;
                                        }
                                    });
                            playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                                @Override
                                public void onNotificationStarted(int notificationId, Notification notification) {
                                    startForeground(notificationId, notification);
                                }

                                @Override
                                public void onNotificationCancelled(int notificationId) {
                                    stopSelf();
                                }
                            });
                            playerNotificationManager.setPlayer(player);
                       // }
                       /* else{
                            Log.e("ERRRRRR","ERRROR");
                        }*/
                    }
                }
            };
            ye.extract(youtubeLink, true, true);
        }catch(Exception e){
        }
    }
}
class HttpGetRequest extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... params){
        String stringUrl = params[0];
        String result = null;
        String inputLine;

        try {
            //Create a URL object holding our url
            URL myUrl = new URL(stringUrl);
            //Create a connection
            HttpURLConnection connection =(HttpURLConnection)
                    myUrl.openConnection();
            //Set methods and timeouts
            connection.setRequestMethod("GET");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);

            //Connect to our url
            connection.connect();
            //Create a new InputStreamReader
            InputStreamReader streamReader = new
                    InputStreamReader(connection.getInputStream());
            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            //Check if the line we are reading is not null
            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }
            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();
            //Set our result equal to our stringBuilder
            result = stringBuilder.toString();
            
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
    }
}
class AsyncTaskLoadImage  extends AsyncTask<String, String, Bitmap> {
    private final static String TAG = "AsyncTaskLoadImage";
    @Override
    protected Bitmap doInBackground(String... params) {
        Bitmap bitmap = null;
        try {
            URL url = new URL(params[0]);
            bitmap = BitmapFactory.decodeStream((InputStream)url.getContent());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return bitmap;
    }
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
    }
}
