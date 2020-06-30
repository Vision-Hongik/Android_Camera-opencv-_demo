package com.example.vision;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import java.util.HashMap;
import java.util.Map;

public class MatRequest extends StringRequest {
    //web 주소
    private static final String LOGIN_REQUEST_URL = IpPath.WEBIP + "/post";
    // string,string 해쉬맵
    private Map<String, String> params;

    public MatRequest(String frame, Response.Listener<String> listener) {
        super(Method.POST, LOGIN_REQUEST_URL, listener, null);
        params = new HashMap<>();
        //매개변수 저장 , key : value
        params.put("frame", frame);
    }

    //매개변수
    @Override
    public Map<String, String> getParams() {
        return params;
    }
}