package com.op.back.petsitter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.op.back.petsitter.dto.TmapDTO;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TmapService {

    private static final String TMAP_URL =
            "https://apis.openapi.sk.com/tmap/geo/convertAddress";

    @Value("${tmap.api.key}")
    private String appKey;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TmapDTO convertAddressToLatLng(String address) {

        try {
            HttpUrl url = HttpUrl.parse(TMAP_URL).newBuilder()
                    .addQueryParameter("version", "1")
                    .addQueryParameter("searchTypCd", "NtoO")
                    .addQueryParameter("reqAdd", address) // OkHttp가 인코딩
                    .addQueryParameter("reqMulti", "S")
                    .addQueryParameter("resCoordType", "WGS84GEO")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("accept", "application/json")
                    .addHeader("appKey", appKey)
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IllegalStateException("Tmap API 호출 실패");
                }

                String body = response.body().string();

                JsonNode root = objectMapper.readTree(body);

                JsonNode addressArray =
                        root.path("ConvertAdd")
                                .path("newAddressList")
                                .path("newAddress");

                if (!addressArray.isArray() || addressArray.size() == 0) {
                    throw new IllegalArgumentException("주소를 좌표로 변환할 수 없습니다.");
                }

                JsonNode newAddress = addressArray.get(0);

                double lat = newAddress.path("newLat").asDouble();
                double lng = newAddress.path("newLon").asDouble();

                return new TmapDTO(lat, lng);
            }

        } catch (Exception e) {
            throw new RuntimeException("Tmap 주소 변환 실패", e);
        }
    }
}
