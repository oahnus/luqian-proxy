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

    private static final long ONE_DAY = 60 * 60 * 24 * 1000;
    private static final long ONE_GB = 1024 * 1024 * 1024;
    private static final long ONE_MB = 1024 * 1024;
    private static final long ONE_KB = 1024;

    public List<StatMeasure> queryList(StatMeasure params) {
        QueryBuilder qb = new QueryBuilder(StatMeasure.class);
        Date date = params.getDate();
        if (date != null) {
            qb.eq("date", date);
        }

        return selectList(qb);
    }


    public Statistics genStatReport(Long sysUserId) {
        int statPeriod = 7;
        LocalDate beforeNDay = LocalDate.now().minusDays(statPeriod);
        Date beforeNDate = DateUtils.localDate2date(beforeNDay);
        List<StatMeasure> measureList = selectList(new QueryBuilder(StatMeasure.class)
                .eq("userId", sysUserId)
                .greaterEqThan("date", beforeNDate));

        Statistics statistics = new Statistics();

        Map<Date, List<StatMeasure>> map = MyCollectionUtils
                .groupList2Map(measureList, "date", Date.class);

        List<Statistics.StatItem> statItemList = new ArrayList<>();

        long total = 0;
        for (int i = 0; i < statPeriod; i++) {
            LocalDate date = beforeNDay.plusDays(i);
            Date key = DateUtils.localDate2date(date);

            Statistics.StatItem item = new Statistics.StatItem();
            item.setDate(key);
            if (map.containsKey(key)) {
                long totalIn = 0, totalOut = 0;
                List<StatMeasure> measures = map.get(key);
                for (StatMeasure measure : measures) {
                    Long inBytes = measure.getInTrafficBytes();
                    Long outBytes = measure.getOutTrafficBytes();
                    totalIn += inBytes;
                    totalOut += outBytes;

                    total = total + inBytes + outBytes;
                }
                item.setInBytes(totalIn);
                item.setOutBytes(totalOut);
            } else {
                item.setInBytes(0);
                item.setOutBytes(0);
            }
            statItemList.add(item);
        }
        // 计算平均流量
        long avgBytes = total / 7;

        Statistics.StatUnit unit;
        long unitVal;
        if (avgBytes < ONE_KB) {
            unit = Statistics.StatUnit.B;
            unitVal = 1;
        } else if (avgBytes < ONE_MB) {
            unit = Statistics.StatUnit.KB;
            unitVal = ONE_KB;
        } else if (avgBytes < ONE_GB) {
            unit = Statistics.StatUnit.MB;
            unitVal = ONE_MB;
        } else {
            unit = Statistics.StatUnit.GB;
            unitVal = ONE_GB;
        }
        statItemList.sort(Comparator.comparingLong(a -> a.getDate().getTime()));
        // 重新计算数据
        for (Statistics.StatItem statItem : statItemList) {
            long inBytes = statItem.getInBytes();
            long outBytes = statItem.getOutBytes();
            statItem.setInBytes(inBytes / unitVal);
            statItem.setOutBytes(outBytes / unitVal);
        }
        statistics.setStatUnit(unit);

        statistics.setDateStats(statItemList);
        return statistics;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void dailyReset() {
        // 重置数据
        List<StatMeasure> yesterdayMeasureList = TrafficMeasureMonitor.reset();
        // 更新进数据库
        List<StatMeasure> needInsertList = new ArrayList<>();
        List<StatMeasure> needUpdateList = new ArrayList<>();

        for (StatMeasure measure : yesterdayMeasureList) {
            if (measure.getId() == null) {
                needInsertList.add(measure);
            } else {
                needUpdateList.add(measure);
            }
        }
        if (!CollectionUtils.isEmpty(needInsertList)) {
            saveBatch(needInsertList);
        }
        if (!CollectionUtils.isEmpty(needInsertList)) {
            updateBatchById(needUpdateList);
        }
    }

    // todo 替换掉Spring Schedule
    @Scheduled(cron = "0 */2 * * * *")
    public void sync() {
        log.info("Sync Measure To Database");
        Iterator<Map.Entry<Integer, StatMeasure>> iterator = TrafficMeasureMonitor.getMeasureIterator();
        List<StatMeasure> insertList = new ArrayList<>();
        List<StatMeasure> updateList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<Integer, StatMeasure> entry = iterator.next();
            StatMeasure measure = entry.getValue();

            // 检查统计对象是否有数据更新
            int syncStatus = measure.getSyncStatus().getAndSet(SyncStatus.SYNCING.ordinal());
            if (syncStatus == SyncStatus.NO_CHANGE.ordinal()) {
                continue;
            }
            if (measure.getId() == null) {
                insertList.add(measure);
            } else {
                updateList.add(measure);
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
