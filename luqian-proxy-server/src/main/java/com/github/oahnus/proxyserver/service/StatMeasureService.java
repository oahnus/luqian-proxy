package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.util.DateUtils;
import com.github.oahnus.luqiancommon.util.MyCollectionUtils;
import com.github.oahnus.proxyserver.dto.Statistics;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.enums.SyncStatus;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import com.github.oahnus.proxyserver.mapper.StatMeasureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;

/**
 * Created by oahnus on 2020-04-30
 * 9:55.
 */
@Service
@Slf4j
public class StatMeasureService extends BaseService<StatMeasureMapper, StatMeasure, Long> {

    private static long ONE_DAY = 60 * 60 * 24 * 1000;
    private static long ONE_GB = 1024 * 1024 * 1024;
    private static long ONE_MB = 1024 * 1024;
    private static long ONE_KB = 1024;

    public List<StatMeasure> queryList(StatMeasure params) {
        QueryBuilder qb = new QueryBuilder(StatMeasure.class);
        Date date = params.getDate();
        if (date != null) {
            qb.eq("date", date);
        }
        List<StatMeasure> measureList = selectList(qb);

        return measureList;
    }


    public Statistics genStatReport(Long sysUserId) {
        Date now = DateUtils.localDate2date(LocalDate.now());
        Date before7Days = DateUtils.localDate2date(LocalDate.now().minusDays(7));
        List<StatMeasure> measureList = selectList(new QueryBuilder(StatMeasure.class)
                .eq("userId", sysUserId)
                .greaterEqThan("date", before7Days));

        Statistics statistics = new Statistics();

        Map<Date, List<StatMeasure>> map = MyCollectionUtils
                .groupList2Map(measureList, "date", Date.class);

        List<Statistics.StatItem> statItemList = new ArrayList<>();

        long total = 0;
        for (Map.Entry<Date, List<StatMeasure>> entry : map.entrySet()) {
            Date key = entry.getKey();
            List<StatMeasure> measures = entry.getValue();
            long totalIn = 0, totalOut = 0;
            Statistics.StatItem item = new Statistics.StatItem();
            item.setDate(key);

            for (StatMeasure measure : measures) {
                Long inBytes = measure.getInTrafficBytes();
                Long outBytes = measure.getOutTrafficBytes();
                totalIn += inBytes;
                totalOut += outBytes;

                total = total + inBytes + outBytes;
            }
            item.setInBytes(totalIn);
            item.setOutBytes(totalOut);
            statItemList.add(item);
        }

        statItemList.sort(Comparator.comparingLong(a -> a.getDate().getTime()));
        statistics.setDateStats(statItemList);

        return statistics;
    }

    // todo 替换掉Spring Schedule
    @Scheduled(cron = "0 */2 * * * *")
    public void sync() {
        log.info("Sync Measure To Database");
        Iterator<Map.Entry<Integer, StatMeasure>> iterator = TrafficMeasureMonitor.getMeasureIterator();
        List<StatMeasure> insertList = new ArrayList<>();
        List<StatMeasure> updateList = new ArrayList<>();
        Date today = DateUtils.localDate2date(LocalDate.now());
        while (iterator.hasNext()) {
            Map.Entry<Integer, StatMeasure> entry = iterator.next();
            Integer key = entry.getKey();
            StatMeasure measure = entry.getValue();

            // 检查统计对象是否有数据更新
            int syncStatus = measure.getSyncStatus().getAndSet(SyncStatus.SYNCING.ordinal());
            if (syncStatus == SyncStatus.NO_CHANGE.ordinal()) {
                continue;
            }
            // 检查是否已存在记录
            StatMeasure statMeasure = selectOne(new QueryBuilder(StatMeasure.class)
                    .eq("date", today)
                    .eq("port", measure.getPort())
                    .eq("appId", measure.getAppId()));

            if (statMeasure == null) {
                insertList.add(measure);
            } else {
                statMeasure.setInTrafficBytes(measure.getInTrafficBytes());
                statMeasure.setOutTrafficBytes(measure.getOutTrafficBytes());
                updateList.add(statMeasure);
            }
            // 复位同步状态
            measure.getSyncStatus().compareAndSet(SyncStatus.SYNCING.ordinal(), SyncStatus.NO_CHANGE.ordinal());
        }
        if (!CollectionUtils.isEmpty(insertList)) {
            saveBatch(insertList);
        }
        if (!CollectionUtils.isEmpty(updateList)) {
            updateBatchById(updateList);
        }
        log.info("Sync Finish");
    }
}
