package com.billy.cc.demo;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.IComponentCallback;
import com.billy.cc.core.component.IDynamicComponent;
import com.billy.cc.core.component.IMainThread;
import com.billy.cc.demo.base.bean.User;

import static com.billy.cc.demo.MainActivity.LoginUserObserverComponent.OBSERVER_ACTION_NAME;

/**
 * @author billy
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String COMPONENT_NAME_A = "demo.ComponentA";

    private TextView textView;
    private TextView loginUserTextView;
    private LoginUserObserverComponent loginUserObserverComponent;
    private TextView loginUserButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loginUserTextView = (TextView) findViewById(R.id.login_user);
        loginUserButton = (TextView) findViewById(R.id.login_user_state_observer);
        textView = (TextView) findViewById(R.id.console);
        addOnClickListeners(R.id.componentAOpenActivity
                , R.id.test_lifecycle
                , R.id.componentAAsyncOpenActivity
                , R.id.componentAGetData
                , R.id.componentAAsyncGetData
                , R.id.componentB
                , R.id.componentBAsync
                , R.id.componentBGetData
                , R.id.componentBLogin
                , R.id.componentKt
                , R.id.test_sub_process
                , R.id.login_user_state_observer
        );
    }

    private void addDynamicComponent() {
        if (loginUserObserverComponent == null) {
            //????????????????????????
            loginUserObserverComponent = new LoginUserObserverComponent();
            //???CC?????????????????????
            // ????????????????????????UserStateManager.onUserLoginStateUpdated()??????????????????CC??????????????????????????????????????????
            CC.registerComponent(loginUserObserverComponent);
            //??????CC??????ComponentB????????????????????????????????????????????????????????????
            CC.obtainBuilder("ComponentB")
                    .setActionName("addLoginObserver")
                    .addParam("componentName", loginUserObserverComponent.getName())
                    .addParam("actionName", OBSERVER_ACTION_NAME)
                    .build()
                    .callAsync();
            loginUserButton.setText(R.string.unobserve_login_user);
        }
    }

    private void removeDynamicComponent() {
        if (loginUserObserverComponent != null) {
            //???CC??????????????????????????????
            CC.unregisterComponent(loginUserObserverComponent);
            //???ComponentB?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            CC.obtainBuilder("ComponentB")
                    .setActionName("removeLoginObserver")
                    .addParam("componentName", loginUserObserverComponent.getName())
                    .build()
                    .callAsync();
            loginUserObserverComponent = null;
            loginUserButton.setText(R.string.observe_login_user);
            loginUserTextView.setText("");
        }
    }

    private void addOnClickListeners(@IdRes int... ids) {
        if (ids != null) {
            for (@IdRes int id : ids) {
                findViewById(id).setOnClickListener(this);
            }
        }
    }
    CC componentBCC;
    @Override
    public void onClick(View v) {
        textView.setText("");
        CCResult result = null;
        CC cc = null;
        switch (v.getId()) {
            case R.id.test_lifecycle:
                CC.obtainBuilder("demo.lifecycle")
                        .build()
                        .callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.componentAOpenActivity:
                cc = CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("showActivityA")
                        .build();
                result = cc.call();
                break;
            case R.id.componentAAsyncOpenActivity:
                CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("showActivityA")
                        .build().callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.componentAGetData:
                cc = CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("getInfo")
                        .build();
                result = cc.call();
                break;
            case R.id.componentAAsyncGetData:
                CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("getInfo")
                        .addInterceptor(new MissYouInterceptor())
                        .build().callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.componentB:
                cc = CC.obtainBuilder("ComponentB")
                        .setActionName("showActivity")
                        .build();
                result = cc.call();
                break;
            case R.id.componentBAsync:
                if (componentBCC != null) {
                    componentBCC.cancel();
                    Toast.makeText(this, R.string.canceled, Toast.LENGTH_SHORT).show();
                } else {
                    componentBCC = CC.obtainBuilder("ComponentB")
                            .setActionName("getNetworkData")
                            .build();
                    componentBCC.callAsyncCallbackOnMainThread(new IComponentCallback() {
                        @Override
                        public void onResult(CC cc, CCResult ccResult) {
                            componentBCC = null;
                            showResult(cc, ccResult);
                        }
                    });
                    Toast.makeText(this, R.string.clickToCancel, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.componentBGetData:
                cc = CC.obtainBuilder("ComponentB")
                        .setActionName("getData")
                        .build();
                result = cc.call();
                break;
            case R.id.componentBLogin:
                CC.obtainBuilder("ComponentB")
                        .setActionName("login")
                        .build()
                        .callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.componentKt:
                CC.obtainBuilder("demo.ktComponent")
                        .setActionName("showActivity")
                        .build()
                        .callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.test_sub_process:
                CC.obtainBuilder("webComponent")
                        .setActionName("openUrl")
                        .setContext(this)
                        .addParam("url", "file:///android_asset/demo.html")
                        .build()
                        .callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.login_user_state_observer:
                if (loginUserObserverComponent == null) {
                    addDynamicComponent();
                } else {
                    removeDynamicComponent();
                }
                break;
            default:
                break;
        }
        if (cc != null && result != null) {
            showResult(cc, result);
        }
    }
    IComponentCallback printResultCallback = new IComponentCallback() {
        @Override
        public void onResult(CC cc, CCResult result) {
            showResult(cc, result);
        }
    };
    private void showResult(CC cc, CCResult result) {
        String text = "result:\n" + JsonFormat.format(result.toString());
        text += "\n\n---------------------\n\n";
        text += "cc:\n" + JsonFormat.format(cc.toString());
        textView.setText(text);
    }

    @Override
    protected void onDestroy() {
        if (componentBCC != null) {
            componentBCC.cancel();
        }
        removeDynamicComponent();
        super.onDestroy();
    }

    /**
     * ???????????????????????????????????????
     */
    class LoginUserObserverComponent implements IDynamicComponent, IMainThread {

        @NonNull String observerComponentName;
        static final String OBSERVER_ACTION_NAME = "loginUserState";

        LoginUserObserverComponent() {
            //????????????????????????ComponentName?????????????????????????????????activity??????????????????????????????
            this.observerComponentName = "mainActivityUserObserver_" + SystemClock.uptimeMillis();
        }

        @Override
        public String getName() {
            return observerComponentName;
        }

        @Override
        public boolean onCall(CC cc) {
            String actionName = cc.getActionName();
            if (OBSERVER_ACTION_NAME.equals(actionName)) {
                //????????????????????????????????????????????????????????????shouldActionRunOnMainThread?????????????????????
                return onLoginUserChanged(cc);
            }
            CC.sendCCResult(cc.getCallId(), CCResult.errorUnsupportedActionName());
            return false;
        }

        private boolean onLoginUserChanged(CC cc) {
            User user = cc.getParamItem("user");
            if (loginUserTextView != null) {
                loginUserTextView.setText(getString(R.string.show_login_user, user == null ? "null" : user.getUserName()));
            }
            CC.sendCCResult(cc.getCallId(), CCResult.success());
            return false;
        }

        @Override
        public Boolean shouldActionRunOnMainThread(String actionName, CC cc) {
            if (OBSERVER_ACTION_NAME.equals(actionName)) {
                //??????observerActionName???????????????onCall????????????????????????
                return true;
            }
            return null;
        }
    }
}
