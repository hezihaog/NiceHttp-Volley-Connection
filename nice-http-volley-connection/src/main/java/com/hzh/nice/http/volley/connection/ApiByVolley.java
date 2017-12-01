package com.hzh.nice.http.volley.connection;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.hzh.nice.http.base.Api;
import com.hzh.nice.http.base.ApiParams;
import com.hzh.nice.http.callback.ApiCallback;
import com.hzh.nice.http.inter.Result;
import com.hzh.nice.http.util.ApiUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Package: com.hzh.nice.http.volley.connection
 * FileName: ApiByVolley
 * Date: on 2017/12/1  下午6:29
 * Auther: zihe
 * Descirbe: NiceHttp Volley实现层
 * Email: hezihao@linghit.com
 */

public class ApiByVolley implements Api {
    private final Handler mainHandler;
    //请求队列
    private final RequestQueue requestQueue;
    private static final int TIME_OUT_SECONDS = 20;

    public ApiByVolley(Context context) {
        mainHandler = new Handler(context.getMainLooper());
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    @Override
    public Result getSync(String url, final ApiParams params, Class clazz) throws Exception {
        ApiUtil.printRequest(url, params);
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(Request.Method.GET, url, future, future) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                super.getParams();
                HashMap<String, String> paramsMap = new HashMap<String, String>();
                final Map<String, ArrayList<String>> paramsList = params.getParams();
                if (paramsList != null) {
                    for (Map.Entry<String, ArrayList<String>> entry : paramsList.entrySet()) {
                        ArrayList<String> values = entry.getValue();
                        if (values != null) {
                            for (int i = 0; i < values.size(); i++) {
                                String value = values.get(i);
                                paramsMap.put(entry.getKey(), value);
                            }
                        }
                    }
                }
                return paramsMap;
            }
        };
        requestQueue.add(request);
        Result res;
        try {
            String result = future.get(TIME_OUT_SECONDS, TimeUnit.SECONDS);
            ApiUtil.printResult(clazz.getName(), result);
            res = ApiUtil.parseResult(clazz, result);
        } catch (Exception e) {
            throw e;
        }
        return res;
    }

    @Override
    public void get(final ApiCallback callback, String url, final ApiParams params, final Class clazz, final String tag) {
        ApiUtil.printRequest(url, params);
        executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    try {
                        callback.onApiStart(tag);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String result) {
                ApiUtil.printResult(clazz.getName(), result);
                Result res;
                try {
                    res = ApiUtil.parseResult(clazz, result);
                    if (callback != null) {
                        final Result finalRes = res;
                        executeRunnable(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    callback.onApiSuccess(finalRes, tag);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callback != null) {
                        executeRunnable(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    callback.onParseError(tag);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                if (callback != null) {
                    executeRunnable(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.onApiFailure(error, 0, "网络错误", tag);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                super.getParams();
                HashMap<String, String> paramsMap = new HashMap<String, String>();
                final Map<String, ArrayList<String>> paramsList = params.getParams();
                if (paramsList != null) {
                    for (Map.Entry<String, ArrayList<String>> entry : paramsList.entrySet()) {
                        ArrayList<String> values = entry.getValue();
                        if (values != null) {
                            for (int i = 0; i < values.size(); i++) {
                                String value = values.get(i);
                                paramsMap.put(entry.getKey(), value);
                            }
                        }
                    }
                }
                return paramsMap;
            }
        };
        request.setTag(tag);
        requestQueue.add(request);
    }

    @Override
    public Result postSync(String url, final ApiParams params, Class clazz) throws Exception {
        ApiUtil.printRequest(url, params);
        RequestFuture<String> future = RequestFuture.newFuture();
        HashMap<String, Object> paramsMap = new HashMap<String, Object>();
        //添加普通String类型参数
        final Map<String, ArrayList<String>> paramsList = params.getParams();
        if (paramsList != null) {
            for (Map.Entry<String, ArrayList<String>> entry : paramsList.entrySet()) {
                ArrayList<String> values = entry.getValue();
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        String value = values.get(i);
                        paramsMap.put(entry.getKey(), value);
                    }
                }
            }
        }
        //添加File文件参数
        Map<String, ArrayList<File>> files = params.getFiles();
        ArrayList<FileEntity> fileEntityList = null;
        if (files != null) {
            fileEntityList = new ArrayList<FileEntity>();
            for (Map.Entry<String, ArrayList<File>> entry : files.entrySet()) {
                ArrayList<File> filesList = entry.getValue();
                if (filesList != null) {
                    for (int i = 0; i < filesList.size(); i++) {
                        File file = filesList.get(i);
                        if (file != null && file.exists()) {
                            FileEntity fileEntity = new FileEntity();
                            fileEntity.mName = entry.getKey();
                            fileEntity.mFileName = file.getName();
                            fileEntity.mFile = file;
                            fileEntityList.add(fileEntity);
                        }
                    }
                }
            }
        }
        MultipartRequest request = new MultipartRequest(url, paramsMap, fileEntityList, future, future);
        requestQueue.add(request);
        Result res;
        try {
            String result = future.get(TIME_OUT_SECONDS, TimeUnit.SECONDS);
            ApiUtil.printResult(clazz.getName(), result);
            res = ApiUtil.parseResult(clazz, result);
        } catch (Exception e) {
            throw e;
        }
        return res;
    }

    @Override
    public void post(final ApiCallback callback, String url, final ApiParams params, final Class clazz, final String tag) {
        ApiUtil.printRequest(url, params);
        //执行请求的准备
        executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    try {
                        callback.onApiStart(tag);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        HashMap<String, Object> paramsMap = new HashMap<String, Object>();
        final Map<String, ArrayList<String>> paramsList = params.getParams();
        //添加普通String类型参数
        if (paramsList != null) {
            for (Map.Entry<String, ArrayList<String>> entry : paramsList.entrySet()) {
                ArrayList<String> values = entry.getValue();
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        String value = values.get(i);
                        paramsMap.put(entry.getKey(), value);
                    }
                }
            }
        }
        //添加文件类型参数
        Map<String, ArrayList<File>> files = params.getFiles();
        ArrayList<FileEntity> fileEntityList = null;
        if (files != null) {
            fileEntityList = new ArrayList<FileEntity>();
            for (Map.Entry<String, ArrayList<File>> entry : files.entrySet()) {
                ArrayList<File> filesList = entry.getValue();
                if (filesList != null) {
                    for (int i = 0; i < filesList.size(); i++) {
                        File file = filesList.get(i);
                        if (file != null && file.exists()) {
                            FileEntity fileEntity = new FileEntity();
                            fileEntity.mName = entry.getKey();
                            fileEntity.mFileName = file.getName();
                            fileEntity.mFile = file;
                            fileEntityList.add(fileEntity);
                        }
                    }
                }
            }
        }
        MultipartRequest request = new MultipartRequest(url, paramsMap, fileEntityList, new Response.Listener<String>() {
            @Override
            public void onResponse(String result) {
                ApiUtil.printResult(clazz.getName(), result);
                Result res;
                try {
                    res = ApiUtil.parseResult(clazz, result);
                    if (callback != null) {
                        final Result finalRes = res;
                        executeRunnable(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    callback.onApiSuccess(finalRes, tag);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callback != null) {
                        executeRunnable(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    callback.onParseError(tag);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                if (callback != null) {
                    executeRunnable(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.onApiFailure(error, 0, "网络错误", tag);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        request.setTag(tag);
        requestQueue.add(request);
    }

    /**
     * 在主线程执行Runnable
     *
     * @param runnable 要执行的runnable实例
     */
    private void executeRunnable(final Runnable runnable) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            });
        }
    }
}
