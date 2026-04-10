package com.mundosvirtuales.visitorassistant.visitorflow;

import android.text.TextUtils;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public class BackendApiClient implements ContactCatalogGateway, NotificationDispatchGateway, MessageDispatchGateway {

    interface VisitorBackendService {
        @GET("contacts")
        Call<List<VisitorDtos.ContactDto>> getContacts();

        @POST("notifications")
        Call<VisitorDtos.NotificationResponseDto> submitNotification(@Body VisitorDtos.NotificationRequestDto request);
    }

    private final VisitorBackendService service;
    private final String deviceId;
    private final String visitorName;
    private final String location;
    private final String configurationErrorMessage;

    public BackendApiClient(String baseUrl,
                            String deviceId,
                            String visitorName,
                            String location,
                            String configurationErrorMessage) {
        this.deviceId = deviceId;
        this.visitorName = visitorName;
        this.location = location;
        this.configurationErrorMessage = configurationErrorMessage;
        this.service = createService(baseUrl);
    }

    BackendApiClient(VisitorBackendService service,
                     String deviceId,
                     String visitorName,
                     String location,
                     String configurationErrorMessage) {
        this.service = service;
        this.deviceId = deviceId;
        this.visitorName = visitorName;
        this.location = location;
        this.configurationErrorMessage = configurationErrorMessage;
    }

    @Override
    public void fetchContacts(final ContactCatalogGateway.Callback callback) {
        if (service == null) {
            callback.onError(configurationErrorMessage);
            return;
        }

        service.getContacts().enqueue(new retrofit2.Callback<List<VisitorDtos.ContactDto>>() {
            @Override
            public void onResponse(Call<List<VisitorDtos.ContactDto>> call, Response<List<VisitorDtos.ContactDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(buildHttpErrorMessage(response.code(), null));
                    return;
                }

                List<VisitorDtos.ContactSummary> contacts = new ArrayList<>();
                for (VisitorDtos.ContactDto dto : response.body()) {
                    contacts.add(dto.toSummary());
                }
                callback.onSuccess(contacts);
            }

            @Override
            public void onFailure(Call<List<VisitorDtos.ContactDto>> call, Throwable throwable) {
                callback.onError("No pude cargar los contactos. Revise la conexión con el backend.");
            }
        });
    }

    @Override
    public void submitNotification(final VisitorDtos.ContactSummary contact, final NotificationDispatchGateway.Callback callback) {
        submitNotificationRequest(new VisitorDtos.NotificationRequestDto(contact.getId(), deviceId, visitorName, location), callback);
    }

    @Override
    public void submitMessageNotification(VisitorDtos.ContactSummary contact, String message, NotificationDispatchGateway.Callback callback) {
        submitNotificationRequest(
                new VisitorDtos.NotificationRequestDto(contact.getId(), deviceId, visitorName, location, "leave_message", message),
                callback
        );
    }

    private void submitNotificationRequest(VisitorDtos.NotificationRequestDto request,
                                           final NotificationDispatchGateway.Callback callback) {
        if (service == null) {
            callback.onError(configurationErrorMessage, false);
            return;
        }

        service.submitNotification(request)
                .enqueue(new retrofit2.Callback<VisitorDtos.NotificationResponseDto>() {
                    @Override
                    public void onResponse(Call<VisitorDtos.NotificationResponseDto> call, Response<VisitorDtos.NotificationResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().toResult());
                            return;
                        }

                        boolean retryable = response.code() >= 500 || response.code() == 408 || response.code() == 504;
                        String message = buildHttpErrorMessage(response.code(), extractErrorDetail(response));
                        callback.onError(message, retryable);
                    }

                    @Override
                    public void onFailure(Call<VisitorDtos.NotificationResponseDto> call, Throwable throwable) {
                        callback.onError("No pude enviar la notificación. Revise la conexión e inténtelo nuevamente.", true);
                    }
                });
    }

    private static VisitorBackendService createService(String baseUrl) {
        if (TextUtils.isEmpty(baseUrl)) {
            return null;
        }

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(baseUrl))
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        return retrofit.create(VisitorBackendService.class);
    }

    private static String ensureTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private static String extractErrorDetail(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            String rawBody = response.errorBody().string();
            if (TextUtils.isEmpty(rawBody)) {
                return null;
            }
            VisitorDtos.ErrorResponseDto error = new Gson().fromJson(rawBody, VisitorDtos.ErrorResponseDto.class);
            if (error != null && !TextUtils.isEmpty(error.detail)) {
                return error.detail;
            }
            return rawBody;
        } catch (IOException exception) {
            return null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String buildHttpErrorMessage(int code, String detail) {
        if (!TextUtils.isEmpty(detail)) {
            return detail;
        }
        if (code == 400) {
            return "La solicitud de notificación no es válida.";
        }
        if (code == 404) {
            return "El backend no encontró el contacto solicitado.";
        }
        if (code == 408 || code == 504) {
            return "El backend demoró demasiado. Inténtelo nuevamente.";
        }
        return "No pude completar la operación con el backend de visitas.";
    }
}
