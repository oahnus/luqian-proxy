package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import com.github.oahnus.proxyserver.mapper.StatMeasureMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Created by oahnus on 2020-04-30
 * 9:55.
 */
@Service
public class StatMeasureService extends BaseService<StatMeasureMapper, StatMeasure, Long> {

    public void sync() {
        Iterator<Map.Entry<Integer, StatMeasure>> iterator = TrafficMeasureMonitor.getMeasureIterator();
        List<StatMeasure> insertList = new ArrayList<>();
        List<StatMeasure> updateList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<Integer, StatMeasure> entry = iterator.next();
            Integer key = entry.getKey();
            StatMeasure measure = entry.getValue();

            Long id = measure.getId();
            if (id == null) {
                insertList.add(measure);
            } else {
                updateList.add(measure);
            }
        }
        if (!CollectionUtils.isEmpty(insertList)) {
            saveBatch(insertList);
        }
        if (!CollectionUtils.isEmpty(updateList)) {
            updateBatchById(updateList);
        }
    }
}
