# SmallPatch
Small插件框架的增量包工具


# 目录结构
- small Small框架的源码
- ypatchcore 用于合成增量包的模块
- oksmall 用于下载服务器增量包生成对应的新版插件包

# 使用：
1：从bundle.json配置确定您要更新的插件uri

```
{
  "version": "1.0.0",
  "bundles": [
    {
      "uri": "main",
      "pkg": "com.chan.app.main",
      "rules": {
        "item": ".ItemActivity"
      }
    },
    {
      "uri": "setting",
      "pkg": "com.chan.app.setting",
      "rules": {
        "index": ".MainActivity"
      }
    }
  ]
}
```
比如这里我们要更新setting


2：合成新的插件包
```
OkSmall.merge(LaunchActivity.this, "setting", "http://192.168.1.100:8080/patch.so");
                Toast.makeText(LaunchActivity.this, "重启应用后更新生效", Toast.LENGTH_SHORT).show();
```


# 原理

详见博客
[何以诚](http://blog.csdn.net/u013022222/article/details/52268526)
