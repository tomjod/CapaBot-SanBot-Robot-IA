package com.mundosvirtuales.visitorassistant.Services;
import com.mundosvirtuales.visitorassistant.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class OpenapiClient {

    private static final String BASE_URL = "https://api.openai.com/";
    private static final String API_KEY = BuildConfig.OPENAI_API_KEY;
    private static final String ORGANIZATION_ID = BuildConfig.ORGANIZATION_ID;
    private static final String PROJECT_ID = BuildConfig.PROJECT_ID;
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Creamos un cliente OkHttpClient para añadir la cabecera de autorización a todas las peticiones.
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Authorization", "Bearer " + API_KEY)
                                .header("OpenAI-Organization", ORGANIZATION_ID)
                                .header("OpenAI-Project", PROJECT_ID)
                                .method(original.method(), original.body());
                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    })
                    .build();

            // Construimos la instancia de Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();
        }
        return retrofit;
    }
}
