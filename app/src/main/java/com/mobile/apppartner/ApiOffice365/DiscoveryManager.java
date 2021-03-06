package com.mobile.apppartner.ApiOffice365;

import android.util.Log;

import com.microsoft.services.discovery.ServiceInfo;
import com.microsoft.services.discovery.fetchers.DiscoveryClient;
import com.microsoft.services.orc.resolvers.ADALDependencyResolver;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

public class DiscoveryManager {

    private static final String TAG = "DiscoveryManager";

    private List<ServiceInfo> mServices;

    public static synchronized DiscoveryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DiscoveryManager();
        }
        return INSTANCE;
    }

    private static DiscoveryManager INSTANCE;

    /**
     * Provides information about the service that corresponds to the provided capability.
     * Gets the info from a local cache.
     * Calls {@link DiscoveryManager#getServiceInfoFromDiscoveryService(String, OperationCallback)}
     * if the service info was not found in cache.
     * @param capability A string that contains the capability of the service that
     *                   is going to be discovered.
     * @param operationCallback The callback to which return the result or error.
     */
    public void getServiceInfo(final String capability, final OperationCallback<ServiceInfo> operationCallback) {
        // Since we're doing considerable work, let's get out of the main thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                // First, look in the locally cached services.
                if(mServices != null) {
                    for (ServiceInfo serviceInfo : mServices) {
                        if (serviceInfo.getCapability().equals(capability)) {
                            Log.i(TAG, "getServiceInfo - " + serviceInfo.getServiceName() + " service for " + capability + " was found in local cached services");
                            operationCallback.onSuccess(serviceInfo);
                            return;
                        }
                    }

                    // We already cached the services but couldn't find the requested service in local cache
                    Log.e(TAG, "getServiceInfo - The " + capability + " capability was not found in the local cached services. "
                            + "Falling back to the discovery service");
                    getServiceInfoFromDiscoveryService(capability, operationCallback);
                } else {
                    // The services have not been cached yet. Go ask the discovery service.
                    getServiceInfoFromDiscoveryService(capability, operationCallback);
                }
            }
        }).start();
    }

    /**
     * Provides information about the service that corresponds to the provided capability.
     * Gets the info from the discovery service.
     * @param capability A string that contains the capability of the service that
     *                   is going to be discovered.
     * @param operationCallback The callback to which return the result or error.
     */
    private void getServiceInfoFromDiscoveryService(final String capability, final OperationCallback<ServiceInfo> operationCallback) {
        try {
            AuthenticationManager.getInstance().setResourceId(Constants.DISCOVERY_RESOURCE_ID);
            ADALDependencyResolver dependencyResolver = (ADALDependencyResolver) AuthenticationManager
                    .getInstance()
                    .getDependencyResolver();

            DiscoveryClient discoveryClient = new DiscoveryClient(Constants.DISCOVERY_RESOURCE_URL, dependencyResolver);

            List<ServiceInfo> services =
                    discoveryClient
                            .getServices()
                            .select("serviceResourceId,serviceEndpointUri,serviceName,capability")
                            .read().get();

            Log.i(TAG, "getServiceInfoFromDiscoveryService - Services discovered\n");
            // Save the discovered services to serve further requests from the local cache.
            mServices = services;

            for (ServiceInfo serviceInfo : services) {
                if (serviceInfo.getCapability().equals(capability)) {
                    // We found the service, send the info to the caller and end this method call
                    Log.i(TAG, "getServiceInfoFromDiscoveryService - " + serviceInfo.getServiceName() + " service for " + capability + " was found in services retrieved from discovery");
                    operationCallback.onSuccess(serviceInfo);
                    return;
                }
            }

            // We haven't cached the services but couldn't find the requested service in discovery service
            NoSuchElementException noSuchElementException = new NoSuchElementException("The " + capability + " capability was not found in the user services.");
            Log.e(TAG, "getServiceInfoFromDiscoveryService - " + noSuchElementException.getMessage());
            operationCallback.onError(noSuchElementException);
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "getServiceInfoFromDiscoveryService - " + e.getMessage());
            operationCallback.onError(e);
        }
    }
}
