package com.eventreminder.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.eventreminder.app.R;
import com.eventreminder.app.data.api.ApiClient;
import com.eventreminder.app.data.api.ApiModels;
import com.eventreminder.app.data.api.ApiService;
import com.eventreminder.app.util.PrefManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etServerIp;
    private TextView tvServerStatus;
    private TextView tabLogin, tabRegister;
    private EditText etUsername, etPassword;
    private Button btnAuth;
    private TextView tvAuthMsg;

    private ApiClient apiClient;
    private ApiService apiService;
    private PrefManager prefs;

    private boolean isLoginTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = new PrefManager(this);

        if (prefs.isLoggedIn()) {
            navigateToMain();
            return;
        }

        apiClient = new ApiClient();
        apiService = new ApiService(apiClient);

        String savedIp = prefs.getServerIp();
        if (!savedIp.isEmpty()) {
            apiClient.setServerIp(savedIp);
        }

        initViews();
    }

    private void initViews() {
        etServerIp = findViewById(R.id.etServerIp);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tabLogin = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnAuth = findViewById(R.id.btnAuth);
        tvAuthMsg = findViewById(R.id.tvAuthMsg);

        String savedIp = prefs.getServerIp();
        if (!savedIp.isEmpty()) {
            etServerIp.setText(savedIp);
        }

        findViewById(R.id.btnTestServer).setOnClickListener(v -> testServer());
        findViewById(R.id.btnClose).setOnClickListener(v -> finishAffinity());

        tabLogin.setOnClickListener(v -> switchTab(true));
        tabRegister.setOnClickListener(v -> switchTab(false));

        btnAuth.setOnClickListener(v -> submitAuth());
    }

    private void testServer() {
        String ip = etServerIp.getText().toString().trim();
        if (ip.isEmpty()) return;

        Button btn = findViewById(R.id.btnTestServer);
        btn.setText("\u2026");
        btn.setEnabled(false);
        tvServerStatus.setText("");

        apiClient.setServerIp(ip);
        prefs.setServerIp(ip);

        apiService.testServer(new ApiService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                btn.setText(getString(R.string.test));
                btn.setEnabled(true);
                if (result) {
                    tvServerStatus.setText(R.string.server_reachable);
                    tvServerStatus.setTextColor(getColor(R.color.success));
                } else {
                    tvServerStatus.setText("Server error");
                    tvServerStatus.setTextColor(getColor(R.color.danger));
                }
            }

            @Override
            public void onError(String error) {
                btn.setText(getString(R.string.test));
                btn.setEnabled(true);
                tvServerStatus.setText(error);
                tvServerStatus.setTextColor(getColor(R.color.danger));
            }
        });
    }

    private void switchTab(boolean login) {
        isLoginTab = login;
        if (login) {
            tabLogin.setBackgroundResource(R.drawable.bg_tab_active);
            tabLogin.setTextColor(getColor(R.color.black));
            tabRegister.setBackgroundResource(R.drawable.bg_tab_inactive);
            tabRegister.setTextColor(getColor(R.color.text_muted));
            btnAuth.setText(R.string.sign_in);
        } else {
            tabRegister.setBackgroundResource(R.drawable.bg_tab_active);
            tabRegister.setTextColor(getColor(R.color.black));
            tabLogin.setBackgroundResource(R.drawable.bg_tab_inactive);
            tabLogin.setTextColor(getColor(R.color.text_muted));
            btnAuth.setText(R.string.create_account);
        }
        tvAuthMsg.setText("");
    }

    private void submitAuth() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String ip = etServerIp.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.fill_fields));
            return;
        }

        if (ip.isEmpty()) {
            showError("Please enter a server IP.");
            return;
        }

        apiClient.setServerIp(ip);
        prefs.setServerIp(ip);

        btnAuth.setEnabled(false);
        btnAuth.setText("\u2026");
        tvAuthMsg.setText("");

        if (isLoginTab) {
            apiService.login(username, password, new ApiService.Callback<ApiModels.AuthResponse>() {
                @Override
                public void onSuccess(ApiModels.AuthResponse result) {
                    btnAuth.setEnabled(true);
                    btnAuth.setText(R.string.sign_in);
                    if (result.success) {
                        prefs.setUserId(result.userId);
                        prefs.setUsername(username);
                        navigateToMain();
                    } else {
                        String err = result.error != null ? result.error : getString(R.string.invalid_credentials);
                        showError(err);
                    }
                }

                @Override
                public void onError(String error) {
                    btnAuth.setEnabled(true);
                    btnAuth.setText(R.string.sign_in);
                    showError(error);
                }
            });
        } else {
            apiService.register(username, password, new ApiService.Callback<ApiModels.AuthResponse>() {
                @Override
                public void onSuccess(ApiModels.AuthResponse result) {
                    if (result.success) {
                        showSuccess(getString(R.string.account_created));
                        btnAuth.postDelayed(() -> {
                            apiService.login(username, password, new ApiService.Callback<ApiModels.AuthResponse>() {
                                @Override
                                public void onSuccess(ApiModels.AuthResponse loginResult) {
                                    btnAuth.setEnabled(true);
                                    btnAuth.setText(R.string.create_account);
                                    if (loginResult.success) {
                                        prefs.setUserId(loginResult.userId);
                                        prefs.setUsername(username);
                                        navigateToMain();
                                    } else {
                                        showError(getString(R.string.invalid_credentials));
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    btnAuth.setEnabled(true);
                                    btnAuth.setText(R.string.create_account);
                                    showError(error);
                                }
                            });
                        }, 800);
                    } else {
                        btnAuth.setEnabled(true);
                        btnAuth.setText(R.string.create_account);
                        String err = result.error != null ? result.error : "Registration failed";
                        showError(err);
                    }
                }

                @Override
                public void onError(String error) {
                    btnAuth.setEnabled(true);
                    btnAuth.setText(R.string.create_account);
                    showError(error);
                }
            });
        }
    }

    private void showError(String msg) {
        tvAuthMsg.setText(msg);
        tvAuthMsg.setTextColor(getColor(R.color.danger));
    }

    private void showSuccess(String msg) {
        tvAuthMsg.setText(msg);
        tvAuthMsg.setTextColor(getColor(R.color.success));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
