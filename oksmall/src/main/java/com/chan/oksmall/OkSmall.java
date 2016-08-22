package com.chan.oksmall;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.chan.ypatchcore.YPatch;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.wequick.small.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by chan on 16/8/19.
 */
public class OkSmall {

    private static final String TAG = "OkSmall";

    /**
     * @param context
     * @param pluginPackageName 插件包名
     * @param uri 远程服务器端增量包的uri
     */
    public static void merge(Context context, String pluginPackageName, String uri) {

        if (TextUtils.isEmpty(pluginPackageName)) {
            Log.e(TAG, "can't parse plugin's package name, check if assets/bundle.json is deleted");
            return;
        }

        // small框架的约定就是，插件名字是以lib+包名.so明明的
        // 比如如果我们插件的包名是com.chan.app.setting
        // 那么对于插件名就是libcom_chan_app_setting.so
        final String soName = "lib" +pluginPackageName.replaceAll("\\.", "_") + ".so";

        //找到旧版本的插件安装包
        final File oldPlugin = new File(context.getApplicationInfo().nativeLibraryDir, soName);
        //新版本都存放到small指定的download目录下
        final File newPlugin = new File(FileUtils.getDownloadBundlePath(), soName);

        //开始下载patch
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(uri).build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {

                //保存从服务器端下载的增量包
                File patch = new File(FileUtils.getDownloadBundlePath(), "patch.so");
                FileOutputStream fileOutputStream = new FileOutputStream(patch);
                byte[] content = response.body().bytes();
                fileOutputStream.write(content);
                fileOutputStream.flush();
                fileOutputStream.close();

                //合成的新插件安装包存放文职有new Plugin指定
                YPatch.patch(oldPlugin.getAbsolutePath(), newPlugin.getAbsolutePath(), patch.getAbsolutePath());
            }
        });
    }
}
