package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.util.DateUtils;
import com.github.oahnus.proxyserver.dto.Statistics;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.entity.SysAccount;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import com.github.oahnus.proxyserver.mapper.StatMeasureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<StatMeasure> queryList(StatMeasure params) {
        QueryBuilder qb = new QueryBuilder(StatMeasure.class);
        Date date = params.getDate();
        if (date != null) {
            qb.eq("date", date);
        }

        return selectList(qb);
    }


    /**
     * 生成首屏统计报告
     * @param sysUserId user id
     * @return 统计信息
     */
    public Statistics genStatReport(Long sysUserId) {
        int statPeriod = 7;
        LocalDate beforeNDay = LocalDate.now().minusDays(statPeriod);
        Date beforeNDate = DateUtils.localDate2date(beforeNDay);
        List<StatMeasure> measureList = selectList(new QueryBuilder(StatMeasure.class)
                .eq("userId", sysUserId)
                .greaterEqThan("date", beforeNDate));

        Statistics statistics = new Statistics();

        Map<String, List<StatMeasure>> map = measureList.stream()
                .collect(Collectors.groupingBy(
                        m -> DateUtils.date2String(m.getDate(), DateUtils.PATTERN_YMD2),
                        Collectors.toList()));

        List<Statistics.StatItem> statItemList = new ArrayList<>();

        long total = 0;
        for (int i = 0; i < statPeriod; i++) {
            LocalDate date = beforeNDay.plusDays(i);
            String key = date.format(formatter);

            Statistics.StatItem item = new Statistics.StatItem();
            item.setDate(DateUtils.string2Date(key, DateUtils.PATTERN_YMD2));
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
                item.setInBytes(BigDecimal.valueOf(totalIn));
                item.setOutBytes(BigDecimal.valueOf(totalOut));
            } else {
                item.setInBytes(BigDecimal.ZERO);
                item.setOutBytes(BigDecimal.ZERO);
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
            BigDecimal inBytes = statItem.getInBytes();
            BigDecimal outBytes = statItem.getOutBytes();

            statItem.setInBytes(inBytes.divide(BigDecimal.valueOf(unitVal), 2, RoundingMode.HALF_UP));
            statItem.setOutBytes(outBytes.divide(BigDecimal.valueOf(unitVal), 2, RoundingMode.HALF_UP));
        }

        long todayInBytes = 0, todayOutBytes = 0;
        int totalConCount = 0;
        Iterator<Map.Entry<Integer, StatMeasure>> iterator = TrafficMeasureMonitor.getMeasureIterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, StatMeasure> entry = iterator.next();
            StatMeasure stat = entry.getValue();
            if (stat.getUserId().equals(sysUserId)) {
                todayInBytes += stat.getInTrafficBytes();
                todayOutBytes += stat.getOutTrafficBytes();
                totalConCount += stat.getConnectCount().get();
            }
        }

        statistics.setTodayInBytes(todayInBytes);
        statistics.setTodayOutBytes(todayOutBytes);
        statistics.setTotalConCount(totalConCount);

        SysAccount account = TrafficMeasureMonitor.getAccount(sysUserId);
        statistics.setTrafficLimit(account.getTrafficLimit());
        statistics.setUsedTraffic(account.getUsedTraffic());

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
        log.info("Sync Statistics Measure To Database");
        Iterator<Map.Entry<Integer, StatMeasure>> iterator = TrafficMeasureMonitor.getMeasureIterator();
        List<StatMeasure> insertList = new ArrayList<>();
        List<StatMeasure> updateList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<Integer, StatMeasure> entry = iterator.next();
            StatMeasure measure = entry.getValue();

            // 检查统计对象是否有数据更新
            if (measure.noChange()) {
                continue;
            }
            if (measure.getId() == null) {
                insertList.add(measure);
            } else {
                updateList.add(measure);
            }
            // 复位同步状态
            measure.resetSync();
        }

        // flush 同步等待列表 （端口冲突的统计对象会被移入同步等待流标）
        List<StatMeasure> measureList = TrafficMeasureMonitor.flushSyncWaitList();
        for (StatMeasure measure : measureList) {
            // 检查统计对象是否有数据更新
            if (measure.noChange()) {
                continue;
            }
            if (measure.getId() == null) {
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
        log.info("Sync Statistics Measure Finish");
    }
}
