package amol.com.AuthMatrix.service;

import amol.com.AuthMatrix.io.ProfileRequest;
import amol.com.AuthMatrix.io.ProfileResponse;

public interface ProfileService {

    ProfileResponse createProfile(ProfileRequest request);

    ProfileResponse getProfile(String email);
} 