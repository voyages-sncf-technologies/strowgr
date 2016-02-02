package com.vsct.dt.haas;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableSet;
import com.vsct.dt.haas.state.*;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class TemplatingTest {

    Haproxy haproxy = new Haproxy("ip_master", "ip_slave");

    @Test
    public void testTemplate() throws IOException {

        //Creation de l'entry point
        EntryPoint entryPoint = new EntryPoint(haproxy, "OCE", "REC1", "hapocer1", "54250", EntryPointStatus.DEPLOYING);

        //Ajout d'un frontend
        Frontend frontend = new Frontend("OCEREC1WS", "50200");
        entryPoint = entryPoint.addFrontend(frontend);
        //Ajout d'un backend
        Backend backend = new Backend("OCEREC1WS");
        entryPoint = entryPoint.addBackend(backend);

        //Ajout d'un serveur
        Server server = new Server("instance_name", "server_name", "10.98.81.74", "9090");
        entryPoint = entryPoint.addServer("OCEREC1WS", server);
        entryPoint = entryPoint.addServerContext("OCEREC1WS", "server_name", "maxconn", "300");

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.mustache").getFile());

        HashMap<String, Object> scopes = entryPoint.toMustacheScope();

        Writer writer = new OutputStreamWriter(System.out);
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new FileReader(file), "no_cache");
        mustache.execute(writer, scopes);
        writer.flush();

    }



}
