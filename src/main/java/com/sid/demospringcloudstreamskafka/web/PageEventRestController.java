package com.sid.demospringcloudstreamskafka.web;

import com.sid.demospringcloudstreamskafka.entities.PageEvent;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;


@RestController
public class PageEventRestController {

    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private InteractiveQueryService interactiveQueryService;


    @GetMapping("/publish/{topic}/{name}")
    public PageEvent publish(@PathVariable String topic,@PathVariable String name){
        PageEvent pageEvent = new PageEvent(name,Math.random()>0.5?"Othmane1":"Othmane2",new Date(), new Random().nextInt(9000));
        streamBridge.send(topic,pageEvent);
        return pageEvent;
    }


    @GetMapping(value = "/analyticsAggregate",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String,Double>> analyticsAggregate(){
        return Flux.interval(Duration.ofSeconds(1))
                .map(seq->{
                    Map<String,Double> map=new HashMap<>();
                    ReadOnlyWindowStore<String, Double> stats = interactiveQueryService.getQueryableStore("total-store", QueryableStoreTypes.windowStore());
                    Instant now=Instant.now();
                    Instant from=now.minusSeconds(30);
                    KeyValueIterator<Windowed<String>, Double> windowedLongKeyValueIterator = stats.fetchAll(from, now);
                    while (windowedLongKeyValueIterator.hasNext()){
                        KeyValue<Windowed<String>, Double> next = windowedLongKeyValueIterator.next();
                        map.put(next.key.key(),next.value);
                    }
                    return map;
                });
    }

}
