package com.vsct.haas.monitoring.aggregator;

import com.vsct.haas.monitoring.aggregator.nsq.NsqLookup;
import com.vsct.haas.monitoring.aggregator.nsq.UnavailableNsqException;

import java.util.Set;

public class AggregatorMain {



    public static void main(String[] args) throws UnavailableNsqException {

        NsqLookup lookup = new NsqLookup("parisiancocktail", 54161);

        Set<String> topics = lookup.getTopics();

        for(String s : topics){
            System.out.println(s);
        }

    }

}
