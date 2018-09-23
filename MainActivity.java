package com.example.home.sociallogin;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static android.R.attr.name;

public class MainActivity extends AppCompatActivity {

    CallbackManager callbackManager;
    LoginButton loginButton;
    TextView t1;
    ImageView i1,i2;
    Button b1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FacebookSdk.sdkInitialize(getApplicationContext());
        loginButton = (LoginButton)findViewById(R.id.login_button);
        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions(Arrays.asList("public_profile","email","user_birthday","user_friends"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
        t1 = (TextView)findViewById(R.id.txtdtls);
        i1 = (ImageView)findViewById(R.id.loginbtn);
        i2 = (ImageView)findViewById(R.id.profilepic);
        b1 = (Button)findViewById(R.id.logoutbtn);
        i1.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
        i2.setVisibility(View.GONE);
        t1.setVisibility(View.GONE);
        b1.setVisibility(View.GONE);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                fblogin();
            }
        });
        i1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               handlelogin();
            }
        });
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlelogout();
            }
        });

    }
    private void fblogin()
    {
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult)
            {
               final ProgressDialog mDialog = new ProgressDialog(MainActivity.this);
                mDialog.setMessage("retreiving data..........");
                mDialog.show();
                String accesstoken = loginResult.getAccessToken().getToken();
                GraphRequest graphRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(), new com.facebook.GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        mDialog.dismiss();
                        getData(object);
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields","id,email,birthday");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
    }

    private void getData(JSONObject object)
    {
        try
        {
            URL profile_picture = new URL("https://graph.facebook.com/"+object.getString("id")+"/picture?width=100&height=100");
            Picasso.with(getApplicationContext()).load(profile_picture.toString()).into(i2);
            String s1 = object.getString("email");
            String s2 = object.getString("birthday");
            StringBuilder ss = new StringBuilder();
            ss.append(s1);
            ss.append("\n\n");
            ss.append(s2);
            ss.append("\n\n");
            t1.setText(ss);
            loginButton.setVisibility(View.VISIBLE);
            i1.setVisibility(View.GONE);
            i2.setVisibility(View.VISIBLE);
            t1.setVisibility(View.VISIBLE);
            b1.setVisibility(View.GONE);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void handlelogin()
    {
        LISessionManager.getInstance(getApplicationContext()).init(this, buildScope(), new AuthListener() {
            @Override
            public void onAuthSuccess() {
                // Authentication was successful.  You can now do
                // other calls with the SDK.
                i1.setVisibility(View.GONE);
                i2.setVisibility(View.VISIBLE);
                t1.setVisibility(View.VISIBLE);
                b1.setVisibility(View.VISIBLE);
                fetchpersonalinfo();
            }

            @Override
            public void onAuthError(LIAuthError error) {
                // Handle authentication errors
                Log.e("Error", error.toString());
            }
        }, true);
    }

    // Build the list of member permissions our LinkedIn session requires
    private static Scope buildScope() {
        return Scope.build(Scope.R_BASICPROFILE, Scope.W_SHARE,Scope.R_EMAILADDRESS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Add this line to your existing onActivityResult() method
        LISessionManager.getInstance(getApplicationContext()).onActivityResult(this, requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    private  void  handlelogout()
    {
        LISessionManager.getInstance(getApplicationContext()).clearSession();
        i1.setVisibility(View.VISIBLE);
        i2.setVisibility(View.GONE);
        t1.setVisibility(View.GONE);
        b1.setVisibility(View.GONE);
    }

    private void fetchpersonalinfo()
    {
        String url = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,public-profile-url,picture-url,email-address,picture-urls::(original))";

        APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
        apiHelper.getRequest(this, url, new ApiListener() {
            @Override
            public void onApiSuccess(ApiResponse apiResponse) {
                // Success!
                try
                {
                    JSONObject jsonObject = apiResponse.getResponseDataAsJson();
                    String firstName =  jsonObject.getString("firstName");
                    String lastName =  jsonObject.getString("lastName");
                    String pictureUrl =  jsonObject.getString("pictureUrl");
                    String emailAddress =  jsonObject.getString("emailAddress");

                    Picasso.with(getApplicationContext()).load(pictureUrl).into(i2);
                    StringBuilder ss = new StringBuilder();
                    ss.append("First Name:"+firstName);
                    ss.append("\n\n");
                    ss.append("Last Name:"+lastName);
                    ss.append("\n\n");
                    ss.append("email:"+emailAddress);
                    t1.setText(ss);
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

            }

            @Override
            public void onApiError(LIApiError liApiError) {
                // Error making GET request!
                Log.e("Error",liApiError.getMessage());
            }
        });
    }

}

