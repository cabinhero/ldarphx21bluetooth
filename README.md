An android version of LDAR VOC check device of PHX21. All features are transplaned from C# perfectly.

Problems in LDAR detection

◆ The operation area of the device area is large, so it is difficult to monitor and manage the detection process in real time. 

◆ There are many sealing points on site, so it is difficult to accurately locate them. 

◆ There is a large workload of manual data entry, and most enterprises fail to realize real-time automatic data entry.

◆ There detection process needs guidance and cooperation of Party A's personnel.


Solutions
1. The platform grid management the detection area, subdivides the detection area into area sub area floor component sealing point, and helps the detection personnel locate the sealing point to be tested quickly with the picture based guidance mode of the mobile end.

2. From the drafting of testing plan to the distribution of testing tasks - on-site testing - Data Submission - retest, a set of marked testing process is formed.

3. Based on 4G and 5g network transmission in the future, automatic data collection and reporting can be realized. Only one person can complete the detection point positioning, data collection and entry, greatly reducing the labor volume and reducing the labor cost.

4. Through the virtual equipment number, the detection task is separated from the specific detection equipment, and the utilization rate of the detection instrument is improved.

Constitutions

The system includes PC terminal and mobile terminal.

[PC] 

Function: mainly for basic account data maintenance, test plan and schedule management.

Technology: react + springboot

Advantages: front and back end separation, suitable for cloud deployment

[Mobile] 

Function: complete the test according to the test task issued by PC

Technology: react native Android hybrid architecture, which encapsulates the underlying Java under the Android platform for phx21

Advantages: in line with the mainstream development route of mobile, it can realize the function expansion quickly and flexibly

Some screenshots:

1.The command panel of connecting bluetooth and data moniting.

![Image text](https://raw.githubusercontent.com/cabinhero/ldarphx21bluetooth/master/docs/monitor.png)
2.The value setting of background.

![Image text](https://raw.githubusercontent.com/cabinhero/ldarphx21bluetooth/master/docs/backgroundvalue.png)
3.The calibration panel.

![Image text](https://raw.githubusercontent.com/cabinhero/ldarphx21bluetooth/master/docs/caculate.png)
4.The panel of task to be checked.

![Image text](https://raw.githubusercontent.com/cabinhero/ldarphx21bluetooth/master/docs/tasks.png)
5.The check panel. Datas are submited to the data server in time.

![Image text](https://raw.githubusercontent.com/cabinhero/ldarphx21bluetooth/master/docs/checkdata.png)
