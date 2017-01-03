package com.github.jksiezni.permissive;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PermissiveTest {

    private static final String TEST_GRANTED_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String TEST_DENIED_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    @Mock
    Context mockContext;

    @Before
    public void setup() {
        when(mockContext.checkPermission(TEST_GRANTED_PERMISSION, 0, 0))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mockContext.checkPermission(TEST_DENIED_PERMISSION, 0, 0))
                .thenReturn(PackageManager.PERMISSION_DENIED);
    }

    @Test
    public void testCheckPermission() {
        assertThat(Permissive.checkPermission(mockContext, TEST_GRANTED_PERMISSION), is(true));
        assertThat(Permissive.checkPermission(mockContext, TEST_DENIED_PERMISSION), is(false));
    }

    @Test
    public void testPermissionsFiltering() {
        String[] result = Permissive.filterPermissions(mockContext,
                new String[]{TEST_DENIED_PERMISSION, TEST_GRANTED_PERMISSION},
                PackageManager.PERMISSION_GRANTED);

        assertArrayEquals(result, new String[]{TEST_GRANTED_PERMISSION});

        result = Permissive.filterPermissions(mockContext,
                new String[]{TEST_DENIED_PERMISSION, TEST_GRANTED_PERMISSION},
                PackageManager.PERMISSION_DENIED);

        assertArrayEquals(result, new String[]{TEST_DENIED_PERMISSION});
    }

}
