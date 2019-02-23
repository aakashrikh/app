package com.webixun.shevetdhara;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String KEY = "FISRT_TIME";
    private static final int FILECHOOSER_RESULTCODE = 98;
    private WebView mainActivityWebview;
    private AlertDialog alertDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean firstLoad = true;
    private FilePickerDialog dialog;
    private View noInternetLayout;
    private ViewTreeObserver.OnScrollChangedListener onScrollChangedListener;
    private ValueCallback<Uri[]> mUploadMessage;
    private String LOG_TAG = "DREG";
    private Uri[] results;

    @Override
    protected void onResume() {
        super.onResume();
        if((ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) ){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE},5656);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mainActivityWebview = findViewById(R.id.main_activity_webview);

        noInternetLayout = findViewById(R.id.layout_no_internet);
        alertDialog = new AlertDialog.Builder(this)
                .setView(R.layout.progress_custom_dialog).create();
        Objects.requireNonNull(alertDialog.getWindow()).
                setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));


        swipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(onScrollChangedListener =
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        if (mainActivityWebview.getScrollY() == 0)
                            swipeRefreshLayout.setEnabled(true);
                        else
                            swipeRefreshLayout.setEnabled(false);
                    }
                });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mainActivityWebview != null) {
                    mainActivityWebview.reload();
                }
            }
        });

        boolean firstTime = SharedPreferenceHelper.getSharedPreferenceBoolean(this, KEY, true);

        if (firstTime) {
            mainActivityWebview.loadUrl("https://www.shvetdhara.com/app/register");
            SharedPreferenceHelper.setSharedPreferenceBoolean(this, KEY, false);
        } else {
            mainActivityWebview.loadUrl("https://www.shvetdhara.com/app/index");
        }


        final WebSettings webSettings = mainActivityWebview.getSettings();
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);

        mainActivityWebview.setLayerType(View.LAYER_TYPE_HARDWARE, null);


        webSettings.setJavaScriptEnabled(true);

        mainActivityWebview.setWebViewClient(new WebViewClient() {
                                                 public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                                                     //Users will be notified in case there's an error (i.e. no internet connection)
                                                     // Toast.makeText(this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
                                                 }

                                                 @Override
                                                 public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                                                     return super.shouldOverrideUrlLoading(view, request);

                                                 }
                                                 //function for make call in
                                                 public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                                     if (url.startsWith("tel:")) {
                                                         Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                                                         startActivity(intent);
                                                         return true;
                                                     }
                                                     return false;

                                                 }
                                                 @Override
                                                 public void onPageStarted(WebView view, String url, Bitmap favicon) {
                                                     if (!DetectConnection.checkInternetConnection(getApplicationContext())) {
                                                         noInternetLayout.setVisibility(View.VISIBLE);
                                                         mainActivityWebview.setVisibility(View.GONE);
                                                         //  Toast.makeText(getApplicationContext(),"No Internet",Toast.LENGTH_SHORT).show();

                                                     } else {
                                                         showLoadingDialog();
                                                         // Toast.makeText(getApplicationContext(),"Internet",Toast.LENGTH_SHORT).show();
                                                         new Handler().postDelayed(new Runnable() {
                                                             @Override
                                                             public void run() {
                                                                 noInternetLayout.setVisibility(View.GONE);
                                                                 mainActivityWebview.setVisibility(View.VISIBLE);
                                                             }
                                                         }, 1500);

                                                         // mainActivityWebview.loadUrl(view.getUrl());
                                                     }
                                                     super.onPageStarted(view, url, favicon);

                                                 }

                                                 public void onPageFinished(WebView view, String url) {
                                                     swipeRefreshLayout.setRefreshing(false);
                                                     if (firstLoad) {
                                                        // stopLoadingDialog();
                                                         firstLoad = false;
                                                     }
                                                     stopLoadingDialog();
                                                     CookieSyncManager.getInstance().sync();
                                                 }
                                             }
        );

        mainActivityWebview.setWebChromeClient(new WebChromeClient() {


            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
                //mUploadMessage = uploadMsg;
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }
                mUploadMessage = filePathCallback;

                openFileSelectionDialog();


                return true;
            }

            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback)
            {
                callback.invoke(origin, true, false);
            }
        });

    }

/*    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage) return;
            Uri result = intent == null || resultCode != RESULT_OK ? null
                    : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialog != null) {
                        openFileSelectionDialog();
                    }
                } else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(MainActivity.this, "Permission is Required for getting list of files", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onStop() {
        swipeRefreshLayout.getViewTreeObserver().removeOnScrollChangedListener(onScrollChangedListener);

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (mainActivityWebview != null && mainActivityWebview.canGoBack())
            mainActivityWebview.goBack();
        else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to exit?")
                    .setCancelable(false)

                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainActivity.this.finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN);

        }

    }

    private void showLoadingDialog() {
        if (alertDialog != null)
            alertDialog.show();
    }

    private void stopLoadingDialog() {
        if (alertDialog != null)
            alertDialog.dismiss();
    }

    private void openFileSelectionDialog() {

        if (null != dialog && dialog.isShowing()) {
            dialog.dismiss();
        }

        //Create a DialogProperties object.
        final DialogProperties properties = new DialogProperties();

        //Instantiate FilePickerDialog with Context and DialogProperties.
        dialog = new FilePickerDialog(MainActivity.this, properties);
        dialog.setTitle("Select a File");
        dialog.setPositiveBtnName("Select");
        dialog.setNegativeBtnName("Cancel");

        //  properties.selection_mode = DialogConfigs.MULTI_MODE; // for multiple files
        properties.selection_mode = DialogConfigs.SINGLE_MODE; // for single file
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(Environment.getExternalStorageDirectory() + "/");

        //Method handle selected files.
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                results = new Uri[files.length];
                for (int i = 0; i < files.length; i++) {
                    String filePath = new File(files[i]).getAbsolutePath();
                    if (!filePath.startsWith("file://")) {
                        filePath = "file://" + filePath;
                    }
                    results[i] = Uri.parse(filePath);
                    Log.d(LOG_TAG, "file path: " + filePath);
                    Log.d(LOG_TAG, "file uri: " + String.valueOf(results[i]));
                }
                mUploadMessage.onReceiveValue(results);
                mUploadMessage = null;
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (null != mUploadMessage) {
                    if (null != results && results.length >= 1) {
                        mUploadMessage.onReceiveValue(results);
                    } else {
                        mUploadMessage.onReceiveValue(null);
                    }
                }
                mUploadMessage = null;
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (null != mUploadMessage) {
                    if (null != results && results.length >= 1) {
                        mUploadMessage.onReceiveValue(results);
                    } else {
                        mUploadMessage.onReceiveValue(null);
                    }
                }
                mUploadMessage = null;
            }
        });

        dialog.show();

    }
}
