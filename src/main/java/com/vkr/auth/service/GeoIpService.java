package com.vkr.auth.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoIpService {

    private final DatabaseReader geoIpReader;

    public String getGeoLocation(String ip) {
        if (ip == null || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1")) {
            return "localhost";
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            CityResponse response = geoIpReader.city(inetAddress);
            String city = response.getCity().getName();
            String country = response.getCountry().getName();
            if (city != null && country != null) {
                return city + ", " + country;
            } else if (country != null) {
                return country;
            } else {
                return "Unknown";
            }
        } catch (IOException | GeoIp2Exception e) {
            log.warn("Could not determine geolocation for IP {}: {}", ip, e.getMessage());
            return "Unknown";
        }
    }
}