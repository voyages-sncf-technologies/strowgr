package com.vsct.dt.haas.state;

import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.events.AddNewEntryPointEvent;
import com.vsct.dt.haas.events.EntryPointDeployedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class AdminState {

    private Map<String, EntryPoint> entryPoints = new HashMap<>();

    @Subscribe public void addNewEntryPoint(AddNewEntryPointEvent event){
        Optional<EntryPoint> entryPointOptional = this.getEntryPoint(event.getEntryPoint().getApplication(), event.getEntryPoint().getPlatform());

        if(!entryPointOptional.isPresent()){

            this.putEntryPoint(event.getEntryPoint());

            /* TODO Make template and throw to NSQ */
        }
    }

    @Subscribe public void entryPointDeployed(EntryPointDeployedEvent event){

        Optional<EntryPoint> entryPointOptional = this.getEntryPoint(event.getApplication(), event.getPlatform());

        if(entryPointOptional.isPresent()){
            EntryPoint entryPoint = entryPointOptional.get();
            this.putEntryPoint(entryPoint.changeStatus(EntryPointStatus.DEPLOYED));
        }

    }

    /* Test purposes */


    public Optional<EntryPoint> getEntryPoint(String application, String platform) {
        return Optional.ofNullable(entryPoints.get(application+platform));
    }

    public void putEntryPoint(EntryPoint entryPoint) {
        this.entryPoints.put(entryPoint.getApplication()+entryPoint.getPlatform(), entryPoint);
    }
}
