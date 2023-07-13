package com.custom


class SLA {
  def days
  def minutes
  def slaCounter
  def exception_time
  def startScriptTime
  def start
  def end
  def startHour
  def endHour
  def dayOfMonth
  def dateFormatted
  def months
  def holidays
  def workDays
  def noWeekend

  SLA() {
    days = 0
    minutes = 0
    slaCounter = 0
    startScriptTime = new Date().toTimestamp().toString()
    start = Calendar.getInstance();
    end = Calendar.getInstance();
    months = ['01','02','03','04','05','06','07','08','09','10','11','12'];
    holidays = [
      //2019
      '20190101','20190102','20190103','20190104','20190107','20190108','20190308','20190501','20190502','20190503',
      '20190509','20190510','20190612','20191104',

      //2020
      '20200101','20200102','20200103','20200106','20200107','20200108','20200224','20200309','20200501',
      '20200504','20200505','20200511','20200612','20201104','20200701','20200624',

      //2021
      '20210101','20210104','20210105','20210106','20210107','20210108','20210222','20210223','20210308',
      '20210503','20210510','20210614','20211104','20211105','20211231',

      //2022
      '20220103','20220104','20220105','20220106','20220107','20220223','20220307','20220308','20220502',
      '20220503','20220509','20220510','20220613','20221104'
    ];

    workDays = ['20160220','20180428','20180609','20181229','20210220'];
  }

  Integer get_work_mins(st,fn,sh,eh,key,nw) {
    return getTime(st,fn,sh,eh,key,nw);
  }

  Integer get_work_mins(st,fn,sh,eh,key) {
    return getTime(st,fn,sh,eh,key,false);
  }

  Integer get_work_days(st,fn,sh,eh,key,nw) {
    return getDays(st,fn,sh,eh,key,nw);
  }

  Integer get_work_days(st,fn,sh,eh,key) {
    return getDays(st,fn,sh,eh,key,false);
  }

  Integer getTime(st,fn,sh,eh,key,nw) {
    if (st == null || fn == null) return null;

    noWeekend = nw
    startHour = sh
    endHour = eh
    start.setTime(Date.parse('yyyy-MM-dd HH:mm:ss.SSS', st.toString()))
    end.setTime(Date.parse('yyyy-MM-dd HH:mm:ss.SSS', fn.toString()))

    //если start и end один день
    if (isStartDayEqualEndDay()) return getStartEndDayDiff();

    //время в первый и последний день интервала
    if (isWorkingDayStart()) minutes += getDayMinsStart();
    if (isWorkingDayEnd()) minutes += getDayMinsEnd();

    //добавить день к началу периода
    addDayToStart()

    //начало и конец интервала равны, значит интервал составляет 2 дня, ничего больше делать не надо
    if (isStartDayEqualEndDay()) return minutes;

    //вычесть день из конца интервала
    minusDayFromEnd()

    //начало и конец интервала равны, значит интервал составляет 3 дня, нужно посчитать время в втором дне
    if (isStartDayEqualEndDay()) {
      if (isWorkingDayStart()) return minutes + ((endHour - startHour) * 60)
      else return minutes;
    }

    //интервал больше 3 дней
    //пока начало интервала не дойдет до конца
    slaCounter = 0
    while (!isStartDayEqualEndDay()) {
      if (isWorkingDayStart()) days++;
      addDayToStart()
      slaCounter++
      if (slaCounter > 5000) {
        exception_time = new Date().toTimestamp().toString()
        println exception_time+' SLA_TIME_EXCEPTION '+ key + ' Start script time: ' + startScriptTime + ' Counter: ' + slaCounter + ' params st: ' + st.toString() + ' fn: ' + fn.toString()
        return null;
      }
    }
    slaCounter = 0
    //последний день не попадает в while
    if (isWorkingDayStart()) days++;

    return ((days * (endHour - startHour)) * 60) + minutes
  }

  Integer getDays(st,fn,sh,eh,key,nw) {
    if (st == null || fn == null) return null;
    noWeekend = nw
    startHour = sh
    endHour = eh
    start.setTime(Date.parse('yyyy-MM-dd HH:mm:ss.SSS', st.toString()))
    end.setTime(Date.parse('yyyy-MM-dd HH:mm:ss.SSS', fn.toString()))

    //если start и end один день
    if (isStartDayEqualEndDay()) return 1;

    //время в первый и последний день интервала
    if (isWorkingDayStart()) minutes += getDayMinsStart();
    if (isWorkingDayEnd()) minutes += getDayMinsEnd();

    //добавить день к началу периода
    //addDayToStart()

    //вычесть день из конца интервала
    minusDayFromEnd()
    //начало и конец интервала равны, значит интервал составляет 3 дня, нужно посчитать время в втором дне

    //интервал больше 3 дней
    //пока начало интервала не дойдет до конца
    slaCounter = 0
    while (!isStartDayEqualEndDay()) {
      if (isWorkingDayStart()) days++;
      addDayToStart()
      slaCounter++
      if (slaCounter > 5000) {
        exception_time = new Date().toTimestamp().toString()
        println exception_time+' SLA_TIME_EXCEPTION '+ key + ' Start script time: ' + startScriptTime + ' Counter: ' + slaCounter + ' params st: ' + st.toString() + ' fn: ' + fn.toString()
        return null;
      }
    }
    slaCounter = 0
    //последний день не попадает в while
    //if (isWorkingDayStart()) days++;
    //if (isWorkingDayEnd()) days++;
    if (minutes > 0) days++;
    return days
  }

  //true если даты равны
  Boolean isStartDayEqualEndDay() {
    if (start.get(Calendar.DAY_OF_YEAR) == end.get(Calendar.DAY_OF_YEAR) && start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) return true;
    else return false;
  }

  //убирает нерабочее время, если есть
  void normalizeStartDay() {

    //если start до начала рабочего времени
    if (start.get(Calendar.HOUR_OF_DAY) < startHour) {
      start.set(Calendar.HOUR_OF_DAY,startHour)
      start.set(Calendar.MINUTE,0)
    }

    //если start после окончания рабочего времени
    if (start.get(Calendar.HOUR_OF_DAY) >= endHour) {
      start.set(Calendar.HOUR_OF_DAY,endHour)
      start.set(Calendar.MINUTE,0)
    }
  }

  //убирает нерабочее время, если есть
  void normalizeEndDay() {

    //если end до начала рабочего времени
    if (end.get(Calendar.HOUR_OF_DAY) < startHour) {
      end.set(Calendar.HOUR_OF_DAY,startHour)
      end.set(Calendar.MINUTE,0)
    }

    //если end после окончания рабочего времени
    if (end.get(Calendar.HOUR_OF_DAY) >= endHour) {
      end.set(Calendar.HOUR_OF_DAY,endHour)
      end.set(Calendar.MINUTE,0)
    }
  }

  //возвращает разницу между start и end, если start и end это один день
  Integer getStartEndDayDiff() {

    //если это рабочий день
    if (isWorkingDayStart()) {
      normalizeStartDay()
      normalizeEndDay()
      return ((end.get(Calendar.HOUR_OF_DAY) - start.get(Calendar.HOUR_OF_DAY))*60) + (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE))
    }
    else return 0;
  }

  //сколько времени прошло от start до конца рабочего дня
  Integer getDayMinsStart() {
    if (isWorkingDayStart()) {
      normalizeStartDay()
      return ((endHour - start.get(Calendar.HOUR_OF_DAY))*60) - start.get(Calendar.MINUTE)
    }
    else return 0;
  }

  //сколько времени прошло от end до начала рабочего дня
  Integer getDayMinsEnd() {
    if (isWorkingDayEnd()) {
      normalizeEndDay()
      return ((end.get(Calendar.HOUR_OF_DAY) - startHour)*60) + end.get(Calendar.MINUTE)
    }
    else return 0;
  }

  //true если рабочий день
  //true если noWeekend == true
  Boolean isWorkingDayStart() {
    if (noWeekend) return true;
    if ((start.get(Calendar.DAY_OF_WEEK) != 7 && start.get(Calendar.DAY_OF_WEEK) != 1 && !isHolidayStart()) || isWorkStart()) return true;
    else return false;
  }

  //true если рабочий день
  //true если noWeekend == true
  Boolean isWorkingDayEnd() {
    if (noWeekend) return true;
    if ((end.get(Calendar.DAY_OF_WEEK) != 7 && end.get(Calendar.DAY_OF_WEEK) != 1 && !isHolidayEnd()) || isWorkEnd()) return true;
    else return false;
  }

  //добавляет день к start
  void addDayToStart() {
    start.set(Calendar.DAY_OF_YEAR,start.get(Calendar.DAY_OF_YEAR)+1)
  }

  //вычетает день из end
  void minusDayFromEnd() {
    end.set(Calendar.DAY_OF_YEAR,end.get(Calendar.DAY_OF_YEAR)-1)
  }

  //если праздник возвращает true
  Boolean isHolidayStart() {
    if (start.get(Calendar.DAY_OF_MONTH) < 10) dayOfMonth = '0'+start.get(Calendar.DAY_OF_MONTH);
    else dayOfMonth = start.get(Calendar.DAY_OF_MONTH).toString();
    dateFormatted = start.get(Calendar.YEAR).toString()+months[start.get(Calendar.MONTH)]+dayOfMonth
    if (holidays.contains(dateFormatted)) return true;
  }

  //если праздник возвращает true
  Boolean isHolidayEnd() {
    if (end.get(Calendar.DAY_OF_MONTH) < 10) dayOfMonth = '0'+end.get(Calendar.DAY_OF_MONTH);
    else dayOfMonth = end.get(Calendar.DAY_OF_MONTH).toString();
    dateFormatted = end.get(Calendar.YEAR).toString()+months[end.get(Calendar.MONTH)]+dayOfMonth
    if (holidays.contains(dateFormatted)) return true;
  }

  //если перенесенный рабочий день возвращает true
  Boolean isWorkStart() {
    if (start.get(Calendar.DAY_OF_MONTH) < 10) dayOfMonth = '0'+start.get(Calendar.DAY_OF_MONTH);
    else dayOfMonth = start.get(Calendar.DAY_OF_MONTH).toString();
    dateFormatted = start.get(Calendar.YEAR).toString()+months[start.get(Calendar.MONTH)]+dayOfMonth
    if (workDays.contains(dateFormatted)) return true;
  }

  //если перенесенный рабочий день возвращает true
  Boolean isWorkEnd() {
    if (end.get(Calendar.DAY_OF_MONTH) < 10) dayOfMonth = '0'+end.get(Calendar.DAY_OF_MONTH);
    else dayOfMonth = end.get(Calendar.DAY_OF_MONTH).toString();
    dateFormatted = end.get(Calendar.YEAR).toString()+months[end.get(Calendar.MONTH)]+dayOfMonth
    if (workDays.contains(dateFormatted)) return true;
  }
}
