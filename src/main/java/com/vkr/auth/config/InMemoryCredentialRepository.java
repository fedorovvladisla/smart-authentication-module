package com.vkr.auth.config;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.UserIdentity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCredentialRepository implements CredentialRepository {

    private final Map<String, Set<PublicKeyCredentialDescriptor>> credentialIdsByUsername = new ConcurrentHashMap<>();
    private final Map<ByteArray, RegisteredCredential> credentialsById = new ConcurrentHashMap<>();
    private final Map<ByteArray, UserIdentity> userHandles = new ConcurrentHashMap<>();

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return credentialIdsByUsername.getOrDefault(username, Collections.emptySet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userHandles.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equals(username))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.ofNullable(userHandles.get(userHandle))
                .map(UserIdentity::getName);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return Optional.ofNullable(credentialsById.get(credentialId));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        RegisteredCredential credential = credentialsById.get(credentialId);
        return credential != null ? Set.of(credential) : Collections.emptySet();
    }

    // Вспомогательный метод для добавления регистрации (можно использовать в WebAuthnService)
    public void addRegistration(RegisteredCredential registration, UserIdentity userIdentity) {
        credentialsById.put(registration.getCredentialId(), registration);
        credentialIdsByUsername.computeIfAbsent(userIdentity.getName(), k -> ConcurrentHashMap.newKeySet())
                .add(PublicKeyCredentialDescriptor.builder()
                        .id(registration.getCredentialId())
                        .build());
        userHandles.put(userIdentity.getId(), userIdentity);
    }
}