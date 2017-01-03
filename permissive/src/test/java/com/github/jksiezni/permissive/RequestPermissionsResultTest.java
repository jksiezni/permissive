package com.github.jksiezni.permissive;

import android.Manifest;
import android.content.pm.PackageManager;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */

public class RequestPermissionsResultTest {

    private final String[] permissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCOUNT_MANAGER
    };

    @Test
    public void testBadArguments() {
        try {
            new RequestPermissionsResult(permissions, new int[]{
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED,
                    0 // redundant element
            });
            fail("Number of arguments is not equal.");
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testGrantedPermissions() {
        RequestPermissionsResult request = new RequestPermissionsResult(permissions, new int[]{
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED
        });
        assertArrayEquals(permissions, request.grantedPermissions);
        assertTrue(request.refusedPermissions.length == 0);
    }

    @Test
    public void testRefusedPermissions() {
        RequestPermissionsResult request = new RequestPermissionsResult(permissions, new int[]{
                PackageManager.PERMISSION_DENIED,
                PackageManager.PERMISSION_DENIED,
                PackageManager.PERMISSION_DENIED,
                PackageManager.PERMISSION_DENIED
        });
        assertArrayEquals(permissions, request.refusedPermissions);
        assertTrue(request.grantedPermissions.length == 0);
    }

    @Test
    public void testSplittingPermissions() {
        RequestPermissionsResult request = new RequestPermissionsResult(permissions, new int[]{
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_DENIED,
                PackageManager.PERMISSION_DENIED
        });
        final String[] expectGranted = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE};
        final String[] expectRefused = {Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCOUNT_MANAGER};
        assertArrayEquals(expectGranted, request.grantedPermissions);
        assertArrayEquals(expectRefused, request.refusedPermissions);
    }
}
