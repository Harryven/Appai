// Home page to get the app user call, app usage information
package com.harryven.appai;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HomePageFragment extends Fragment {
    private View PageView = null;
    private static final String TAG = "HomePage";
    private int MY_PERMISSIONS_REQUEST_CALL_LOG = 10, MY_PERMISSIONS_REQUEST_SMS = 11 ,
            MY_PERMISSIONS_REQUEST_USAGE_STATS = 12;
    private SqlLiteInterface db;
    private long TotalDuration = 0;
    private int TotalSMS = 0 ;
    private long TotalForeGroundTime = 0;
    private long SocialApps = 0, CabApps = 0 ;

    private static final String TABLE_APP_CALLS= "AppCalls_Table";
    private static final String TABLE_APP_SMS= "AppSMS_Table";
    private static final String TABLE_APP_PACKAGE= "AppPackage_Table";
    private static final String TABLE_APP_BROWSER= "AppBrowser_Table";

    private static final String KEY_ID = "id";
    private static final String KEY_Number = "Number";
    private static final String KEY_Name = "Name";
    private static final String KEY_Duration = "Duration";
    private static final String KEY_Type = "Type";
    private static final String KEY_DateofCall = "DateofCall";
    private static final String KEY_DateofSMS = "DateofSMS";
    private static final String KEY_FirstTimeStamp = "FirstTimeStamp";
    private static final String KEY_LastTimeStamp = "LastTimeStamp";
    private static final String KEY_LastTimeUsed = "LastTimeUsed";
    private static final String KEY_ForegroundTime = "ForegroundTime";
    private static final String KEY_Title = "Title";
    private static final String KEY_Url = "Url";
    private static final String KEY_DateSeen = "DateSeen";

    private TextView TotalDurationValue, TotalSMSValue, TotalMobileTimeValue, TotalAppUsageValue ,
            MobileAddictionValue, SocialAppsValue, CabAppsValue, CheckAppStatistics, TotalUsageAppClick;
    private ProgressBar Progressing;

    private static int[] COLORS = new int[] { Color.GREEN, Color.BLUE,Color.MAGENTA, Color.CYAN };
    private CategorySeries mSeries = new CategorySeries("");
    private DefaultRenderer mRenderer = new DefaultRenderer();
    private GraphicalView mChartView;

    private ArrayList<String> ListViewArrayList = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Layout inflation
        PageView = inflater.inflate(R.layout.fragment_home_page, container, false);

        TotalDurationValue = (TextView) PageView.findViewById(R.id.TotalDurationValue);
        TotalMobileTimeValue = (TextView) PageView.findViewById(R.id.TotalMobileTimeValue);
        TotalSMSValue = (TextView) PageView.findViewById(R.id.TotalSMSValue);
        TotalAppUsageValue = (TextView) PageView.findViewById(R.id.TotalAppUsageValue);
        MobileAddictionValue = (TextView) PageView.findViewById(R.id.MobileAddictionValue);
        CabAppsValue = (TextView) PageView.findViewById(R.id.CabAppsValue);
        SocialAppsValue = (TextView) PageView.findViewById(R.id.SocialAppsValue);
        CheckAppStatistics = (TextView) PageView.findViewById(R.id.CheckAppStatistics);
        TotalUsageAppClick = (TextView) PageView.findViewById(R.id.TotalAppUsageClick);
        Progressing = (ProgressBar) PageView.findViewById(R.id.ProgressCredencial);

        //This is for initializing db interface class
        db = new SqlLiteInterface(getActivity());

        //check call log permission, take the date and insert into the database for reports
        checkCallLogPermission();

        // Outgoing SMS History
        checkSMSLogPermission();

        // App Usage History
        checkPackageUsagePermission();

        // Browser History
        BrowserHistory();

        // On click listener for checking app usage statistics
        CheckAppStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                checkPackageUsagePermission();
            }
        });

        // On click listener for getting more info on app usage
        TotalUsageAppClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final Dialog DetDialog = new Dialog(getActivity());
                DetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                DetDialog.setContentView(R.layout.dialog_layout);

                WindowManager.LayoutParams LP = new WindowManager.LayoutParams();
                LP.copyFrom(DetDialog.getWindow().getAttributes());
                LP.width = WindowManager.LayoutParams.MATCH_PARENT;
                LP.height = WindowManager.LayoutParams.MATCH_PARENT;
                DetDialog.getWindow().setAttributes(LP);

                ListView DialogListView= (ListView) DetDialog.findViewById(R.id.DialogListView);

                final ArrayAdapter<String> ListViewAdapter = new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_list_item_1, ListViewArrayList);
                DialogListView.setAdapter(ListViewAdapter);

                DetDialog.show();
            }
        });

        return PageView;
    }

    //this is triggered when user says yes or no to different permissions required to run the statistics
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 10 :
                // CALL LOG
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // call log permission granted
                    int CallLogPermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.READ_CALL_LOG);

                    if (CallLogPermissionCheck == PackageManager.PERMISSION_GRANTED ) {
                        //Call log permission given
                        OnCallPermissionGiven();
                    }
                } else {

                    // Call log permission denied,
                    // alert as this information will not come in the reports
                }
                break;

            case 11 :
                // SMS LOG
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // sms log permission granted
                    int SMSLogPermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.READ_SMS);

                    if (SMSLogPermissionCheck == PackageManager.PERMISSION_GRANTED ) {
                        //SMS permission given
                        OnSMSPermissionGiven();
                    }
                } else {

                    // SMS permission denied,
                    // alert as this information will not come in the reports
                }
                break;

            case 12 :
                // App usage LOG
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // call log permission granted
                    int PackageUsagePermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.PACKAGE_USAGE_STATS);

                    if (PackageUsagePermissionCheck == PackageManager.PERMISSION_GRANTED ) {
                        //package usage permission given
                        OnPackageUsagePermissionGiven();
                    }
                } else {

                    // package permission denied,
                    // alert as this information will not come in the reports
                }
                break;

            default:
                break;
        }
    }

    //check permission for reading call logs
    public void checkCallLogPermission() {
        int CallLogPermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_CALL_LOG);

        if (CallLogPermissionCheck != PackageManager.PERMISSION_GRANTED){
            requestPermissions(
                    new String[]{Manifest.permission.READ_CALL_LOG},
                    MY_PERMISSIONS_REQUEST_CALL_LOG);
        } else {
            // permission already taken
            OnCallPermissionGiven();

            return;
        }
    }

    //check permission for reading SMS logs
    public void checkSMSLogPermission() {
        int SMSLogPermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_SMS);

        if (SMSLogPermissionCheck != PackageManager.PERMISSION_GRANTED){
            requestPermissions(
                    new String[]{Manifest.permission.READ_SMS},
                    MY_PERMISSIONS_REQUEST_SMS);
        } else {
            // permission already taken
            OnSMSPermissionGiven();

            return;
        }
    }

    //check permission for reading package usage
    public void checkPackageUsagePermission() {
        Progressing.setVisibility(View.VISIBLE);

        AppOpsManager appOps = (AppOpsManager) getActivity()
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), getActivity().getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        Log.d(TAG, "GRANTED " + granted);

        if (granted) {
            CheckAppStatistics.setVisibility(View.GONE);
            OnPackageUsagePermissionGiven();
        }
        else {
            Progressing.setVisibility(View.GONE);
            CheckAppStatistics.setVisibility(View.VISIBLE);
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

    }

    // read call logs on permission given
    public void OnCallPermissionGiven() {
        // Getting call log information
        Cursor CallCursor = getActivity().managedQuery(CallLog.Calls.CONTENT_URI, null,null, null, null);

        String Number = "", Name ="", GeoCode = "", DateofCallString = "";
        int Type = 0, DateofCallinSeconds = 0, Duration=0;

        TotalDuration = 0;

        int NameIndex = CallCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int NumberIndex = CallCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int DurationIndex = CallCursor.getColumnIndex(CallLog.Calls.DURATION);
        int TypeIndex = CallCursor.getColumnIndex(CallLog.Calls.TYPE);
        int DateofCallIndex = CallCursor.getColumnIndex(CallLog.Calls.DATE);
        int GeoCodeIndex = CallCursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION);

        while (CallCursor.moveToNext()) {
            Number = CallCursor.getString(NumberIndex);
            Name = CallCursor.getString(NameIndex);
            Duration = CallCursor.getInt(DurationIndex);
            TotalDuration = TotalDuration +Duration;
            Type = CallCursor.getInt(TypeIndex);
            DateofCallinSeconds = CallCursor.getInt(DateofCallIndex);
            if (DateofCallinSeconds != 0) {
                SimpleDateFormat Formatter = new SimpleDateFormat("dd-MM-yy HH:mm");
                DateofCallString = Formatter.format(new Date(DateofCallinSeconds * 1000L));
            }

            GeoCode = CallCursor.getString(GeoCodeIndex);
            Log.d(TAG, "calls info " + Number + " | " + Name + " | " + (double) Duration/60+ " mins" + " | " + Type + " | "
                    + DateofCallString + " | " + GeoCode);
        }

        //Display Total Call Duration
        TotalDurationValue.setText(TotalDuration / 3600 + " hrs");

        //Insert into table
        InsertAppCallsTable(Name, Number, Duration, Type, DateofCallinSeconds);

    }

    // read SMS log on permission given
    public void OnSMSPermissionGiven() {
        Cursor SMSCursor = getActivity().getContentResolver().query(Uri.
                parse("content://sms/sent"), null, null, null, null);

        String Message_Id = "", NumeroTelephone = "", DateofSMSString ="", Name="";
        int DateofSMSinSeconds = 0, Type = 0;
        TotalSMS = SMSCursor.getCount();

        while (SMSCursor.moveToNext()) {
            Message_Id = SMSCursor.getString(SMSCursor.getColumnIndex("_id"));
            Type = SMSCursor.getInt(SMSCursor.getColumnIndex("type"));
            NumeroTelephone= SMSCursor.getString(SMSCursor.getColumnIndex("address")).trim();
            DateofSMSinSeconds = SMSCursor.getInt(SMSCursor.getColumnIndex("date"));
            if (DateofSMSinSeconds != 0) {
                SimpleDateFormat Formatter = new SimpleDateFormat("dd-MM-yy HH:mm");
                DateofSMSString = Formatter.format(new Date(DateofSMSinSeconds * 1000L));
            }


            Log.d(TAG, "SMS Info " +Message_Id + " | " + Type + " | " + NumeroTelephone + " | "
                    + DateofSMSString + " | " + Name);
        }

        //Display total out going SMS
        TotalSMSValue.setText(TotalSMS + "");

        //Insert into table
        InsertAppSMSTable(NumeroTelephone, Type, DateofSMSinSeconds);

    }

    //read browser history
    public void BrowserHistory() {
        Cursor BrowserCursor = getActivity().getContentResolver().query(Uri.
                parse("content://com.android.chrome.browser/history"), null, null, null, null);

        while (BrowserCursor.moveToNext()) {
            long DateSeen = BrowserCursor.getLong(BrowserCursor.getColumnIndex("date"));
            String Url = BrowserCursor.getString(BrowserCursor.getColumnIndex("url"));
            String Title = BrowserCursor.getString(BrowserCursor.getColumnIndex("title"));

            InsertBrowserTable(Title, Url, DateSeen);

            Log.d(TAG, "Browser Info " +DateSeen +" | " +Url +" | " + Title);
        }

    }

    //read apps usage on permission given
    public void OnPackageUsagePermissionGiven() {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getActivity()
                .getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar beginCal = Calendar.getInstance();
        beginCal.set(Calendar.YEAR, -1);

        List<UsageStats> QueryUsageStats = mUsageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, beginCal.getTimeInMillis(),
                        System.currentTimeMillis());
        //initializing the values
        TotalForeGroundTime = 0;
        SocialApps = 0;
        CabApps = 0;

        ListViewArrayList = new ArrayList<String>();

        for (int i =0; i< QueryUsageStats.size(); i++) {
            int minutes = (int) ((QueryUsageStats.get(i).getTotalTimeInForeground() / (1000*60)) % 60);
            int seconds = (int) (QueryUsageStats.get(i).getTotalTimeInForeground() / 1000) % 60 ;
            int hours   = (int) ((QueryUsageStats.get(i).getTotalTimeInForeground() / (1000*60*60))% 24);

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String LastTimeUsedString = formatter.format(new Date(QueryUsageStats.get(i).getLastTimeUsed()));
            String FirstTimeString = formatter.format(new Date(QueryUsageStats.get(i).getFirstTimeStamp()));
            String LastTimeString = formatter.format(new Date(QueryUsageStats.get(i).getLastTimeStamp()));

            //Calculate total foreground time spent on the app
            TotalForeGroundTime = QueryUsageStats.get(i).getTotalTimeInForeground() + TotalForeGroundTime;

            //Insert into table
            InsertAppPackageTable(QueryUsageStats.get(i).getPackageName(), QueryUsageStats.get(i).getFirstTimeStamp(),
                    QueryUsageStats.get(i).getLastTimeStamp(), QueryUsageStats.get(i).getLastTimeUsed(),
                    QueryUsageStats.get(i).getTotalTimeInForeground());

            //add to array for viewing details
            if (hours > 5) {
                ListViewArrayList.add(QueryUsageStats.get(i).getPackageName() + " | " + hours
                        + " hours " + minutes + " minutes " + seconds + " seconds ");
            }

            //for testing and logging purposes
            //if ((double)(QueryUsageStats.get(i).getTotalTimeInForeground()/3600000) > 0) {
            //    Log.d(TAG, "Package info for " + i + " is " + QueryUsageStats.get(i).getPackageName() + " | "
            //            + (double) QueryUsageStats.get(i).getTotalTimeInForeground() / 3600000 + " - "
            //            + hours
            //            + " hours " + minutes + " minutes " + seconds + " seconds " + " | "
            //            + LastTimeUsedString + " | " + FirstTimeString + " | " + LastTimeString + " | " +
            //            QueryUsageStats.get(i).describeContents());
            //}

            //aggregate values for taxi apps and social apps
            switch (QueryUsageStats.get(i).getPackageName()) {
                case "com.google.android.youtube":
                    SocialApps = SocialApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.twitter.android":
                    SocialApps = SocialApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.whatsapp":
                    SocialApps = SocialApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.quora.android":
                    SocialApps = SocialApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.facebook.android":
                    SocialApps = SocialApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.instagram.android":
                    SocialApps = SocialApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.linkedin.android":
                    SocialApps = SocialApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.ubercab":
                    CabApps = CabApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.olacabs.android":
                    CabApps = CabApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
                case "com.merucab.android":
                    CabApps = CabApps + QueryUsageStats.get(i).getTotalTimeInForeground();
                    break;
            }
        }

        //Removing the progress
        if (Progressing != null) {
            Progressing.setVisibility(View.GONE);
        }
        //Display TotalForegroundTime spent on apps
        long TotalForeGroundTimeHrs  = TotalForeGroundTime/3600000;
        long TotalSMShrs = TotalSMS*30;
        long MobileTime = (TotalForeGroundTime/1000)+TotalDuration +(TotalSMShrs);
        long MobileTimeHrs = MobileTime/ 3600;
        long  CabAppsHrs =  CabApps/ 3600000;
        long SocialAppsHrs = SocialApps/ 3600000;

        // set values to be shown on screen
        TotalAppUsageValue.setText(TotalForeGroundTimeHrs + " hrs");
        CabAppsValue.setText(CabAppsHrs + " hrs ");
        SocialAppsValue.setText(SocialAppsHrs + " hrs ");

        if ( MobileTimeHrs/360 >= 8) {
            MobileAddictionValue.setText("High " +MobileTimeHrs/360 + "hrs / day");
        }
        else {
            if (MobileTimeHrs/360 >= 4){
                MobileAddictionValue.setText("Moderate " +MobileTimeHrs/360 + "hrs / day");
            }
            else {
                MobileAddictionValue.setText("Low " +MobileTimeHrs/360 + "hrs / day");
            }
        }
        TotalMobileTimeValue.setText(MobileTimeHrs +" hrs ");

        //draw Graph
        long TotalDurationHrs = TotalDuration / 3600;
        long RestAppHrs = TotalForeGroundTimeHrs - CabAppsHrs - SocialAppsHrs;
        GraphDraw(TotalDurationHrs, SocialAppsHrs, CabAppsHrs, RestAppHrs);

        //send a notification with information regarding the app user statistics
        CustomNotification();

        //
        // this is to demonstrate the use of aggqueryusage stats tp aggregate for the last one week.
        /*
        final Map<String,UsageStats> AggQueryUsageStats = mUsageStatsManager.
                queryAndAggregateUsageStats(beginCal.getTimeInMillis(), System.currentTimeMillis());
        Log.d(TAG, "Agg query total " +AggQueryUsageStats.size());
        for (Map.Entry<String, UsageStats> entry : AggQueryUsageStats.entrySet()) {
            int j = 0 ;
            int minutes = (int) ((entry.getValue().getTotalTimeInForeground() / (1000*60)) % 60);
            int seconds = (int) (entry.getValue().getTotalTimeInForeground() / 1000) % 60 ;
            int hours   = (int) ((entry.getValue().getTotalTimeInForeground() / (1000*60*60))% 24);

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String LastTimeUsedString = formatter.format(new Date(entry.getValue().getLastTimeUsed()));
            String FirstTimeString = formatter.format(new Date(entry.getValue().getFirstTimeStamp()));
            String LastTimeString = formatter.format(new Date(entry.getValue().getLastTimeStamp()));

            Log.d(TAG, "Agg query info " +" | " +entry.getValue().getPackageName() +" | "
                    +(double) entry.getValue().getTotalTimeInForeground()/3600000 + " - "
                    +hours + " hours "  +minutes +" minutes "  +seconds +" seconds "+ " | " +LastTimeUsedString
                    + " | " +FirstTimeString + " | " +LastTimeString );

        }*/

    }

    //plot the usage stats on a pie chart
    public void GraphDraw(long Dur, long Social, long Cab, long Rest) {
        long[] VALUES = new long[] { Dur, Social, Cab, Rest };
        String[] NAME_LIST = new String[] { "Call", "Social", "Taxi", "Rest Apps" };

        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(15);
        mRenderer.setLegendTextSize(15);
        mRenderer.setMargins(new int[] { 20, 30, 15, 0 });
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setStartAngle(90);

        for (int i = 0; i < VALUES.length; i++) {
            mSeries.add(NAME_LIST[i] + " " + VALUES[i], VALUES[i]);
            SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
            renderer.setColor(COLORS[(mSeries.getItemCount() - 1) % COLORS.length]);
            mRenderer.addSeriesRenderer(renderer);
        }

        if (mChartView != null) {
            mChartView.repaint();
        }


        if (mChartView == null) {
            LinearLayout layout = (LinearLayout) PageView.findViewById(R.id.chart);
            mChartView = ChartFactory.getPieChartView(getActivity(), mSeries, mRenderer);
            mRenderer.setClickEnabled(true);
            mRenderer.setSelectableBuffer(10);

            mChartView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();

                    if (seriesSelection == null) {
                        Toast.makeText(getActivity(), "No chart element was clicked", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(), "Chart element data point index " + (seriesSelection.getPointIndex() + 1) + " was clicked" + " point value=" + seriesSelection.getValue(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mChartView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                    if (seriesSelection == null) {
                        Toast.makeText(getActivity(),"No chart element was long pressed", Toast.LENGTH_SHORT);
                        return false;
                    }
                    else {
                        Toast.makeText(getActivity(), "Chart element data point index " + seriesSelection.getPointIndex() + " was long pressed", Toast.LENGTH_SHORT);
                        return true;
                    }
                }
            });
            layout.addView(mChartView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        }
        else {
            mChartView.repaint();
        }

    }

    //insert calls log into local db
    public void InsertAppCallsTable(String Name, String Number, int Duration, int Type, int DateofCall){
        //Storing the data in local database
        ContentValues AppCallsCV = new ContentValues();
        AppCallsCV.put(KEY_Name, Name);
        AppCallsCV.put(KEY_Number, Number);
        AppCallsCV.put(KEY_Duration, Duration);
        AppCallsCV.put(KEY_Type, Type);
        AppCallsCV.put(KEY_DateofCall, DateofCall);
        db.getWritableDatabase().insert(TABLE_APP_CALLS, null, AppCallsCV);
    }

    //insert SMS log into local db
    public void InsertAppSMSTable(String Number, int Type, int DateofSMS){
        //Storing the data in local database
        ContentValues AppSMSCV = new ContentValues();
        AppSMSCV.put(KEY_Number, Number);
        AppSMSCV.put(KEY_Type, Type);
        AppSMSCV.put(KEY_DateofSMS, DateofSMS);
        db.getWritableDatabase().insert(TABLE_APP_SMS, null, AppSMSCV);
    }

    //insert Apps log into local db
    public void InsertAppPackageTable(String Name, long FirstTimeStamp, long LastTimeStamp, long LastTimeUsed,
                                      long ForegroundTime){
        //Storing the data in local database
        ContentValues AppPackageCV = new ContentValues();
        AppPackageCV.put(KEY_Name, Name);
        AppPackageCV.put(KEY_FirstTimeStamp, FirstTimeStamp);
        AppPackageCV.put(KEY_LastTimeStamp, LastTimeStamp);
        AppPackageCV.put(KEY_LastTimeUsed, LastTimeUsed);
        AppPackageCV.put(KEY_ForegroundTime, ForegroundTime);
        db.getWritableDatabase().insert(TABLE_APP_PACKAGE, null, AppPackageCV);
    }

    //insert browser log into local db
    public void InsertBrowserTable(String Title, String Url, long DateSeen){
        //Storing the data in local database
        ContentValues AppBrowserCV = new ContentValues();
        AppBrowserCV.put(KEY_Title, Title);
        AppBrowserCV.put(KEY_Url, Url);
        AppBrowserCV.put(KEY_DateSeen, DateSeen);
        db.getWritableDatabase().insert(TABLE_APP_BROWSER, null, AppBrowserCV);
    }

    //provide notification on completion of app statistics collation
    public void CustomNotification() {
        int icon = R.drawable.ic_launcher;
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, "Mobile Usage Patterns", when);

        NotificationManager notificationManager = (NotificationManager) getActivity().
                getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteViews contentView = new RemoteViews(getActivity().getPackageName(), R.layout.notification_layout);
        contentView.setImageViewResource(R.id.image, R.drawable.ic_launcher);
        contentView.setTextViewText(R.id.title, "Mobile Usage Patterns");
        //contentView.setTextViewText(R.id.text, "Total time on Mobile: \n Total time on SMS: \n Total time on App: ");
        notification.contentView = contentView;
        notification.bigContentView = contentView;

        //Action on clicking the notifcation. For testing purposes triggering the same activity.
        Intent notificationIntent = new Intent(getActivity(), HomePageActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getActivity(), 0, notificationIntent, 0);
        notification.contentIntent = contentIntent;

        notification.flags |= Notification.FLAG_AUTO_CANCEL; //Do not clear the notification
        notification.defaults |= Notification.DEFAULT_LIGHTS; // LED
        notification.defaults |= Notification.DEFAULT_VIBRATE; //Vibration
        notification.defaults |= Notification.DEFAULT_SOUND; // Sound

        //Display notification
        notificationManager.notify(0, notification);
    }

}
