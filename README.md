<p align="center">
	<img src="./docs/images/bridge.png" width="70%">
</p>
<p align="center">
  <a href='https://gitee.com/sammery/bridge-station/stargazers'><img src='https://gitee.com/sammery/bridge-station/badge/star.svg?theme=dark' alt='star'></img></a>
  <a href='https://gitee.com/sammery/bridge-station/members'><img src='https://gitee.com/sammery/bridge-station/badge/fork.svg?theme=dark' alt='fork'></img></a>
</p>

# 软件简介
桥头堡（bridge-station）是一个基于Netty实现的、开源的跨局域网通信平台，实现将局域网内的服务通过内网穿透、公网端口映射等方式搬移到个人电脑或者另一个局域网中，创建局域网之间的私密连接，实现跨局域网合作。

# 软件说明
以伦敦桥为原型，借鉴桥头塔楼的形象，表明程序的意义，即通过通道相连两端桥头堡打开对端的镜像世界，实现两个局域网的无感访问。

> 伦敦桥塔桥两端由石塔连接，桥身分为上、下两层，上层为宽阔的悬空人行道，行人从桥上通过，可以饱览泰晤士河两岸的美丽风光；下层可供车辆通行，桥面可以按需向上折起，船只过后，桥身慢慢落下，恢复车辆通行。

正如程序的处理逻辑，为了保证客户端和服务端之间的数据交互，减少通信耗时，程序中存在两种通道：

- 控制通道
- 数据通道

控制通道相当于上层，一直处于连接状态，身份认证、配置传输、连接请求均通过该通道传输，当身份认证结束，并且客户端无新的连接请求建立，则控制通道处于空闲状态，与服务端进行心跳检测，交互确认配置是否一致。

数据通道相当于下层，有数据传输时连接，建立连接请求的消息通过上层连接进行通知服务端，并建立数据通道，服务端与对端局域网服务建立连接之后与对应的数据通道绑定，实现整条链路联通，当数据交互完毕归还数据通道。

# 软件定位
桥头堡（bridge-station）致力于解决出差、居家办公、网间合作等过程中遇到的网络互访问题，实现与在公司内网、家庭网络等内网中无差异化、一致性的使用体验，并非常规意义上的内网穿透，反而桥头堡的应用场景中存在借助内网穿透应用的情景，与内网穿透不构成竞争关系。

# 软件架构

# 数据时序
<p align="center">
	<img src="./docs/images/数据交互.png">
</p>

# 开发计划
- [x] 实现完整数据交互。
- [x] 实现数据库管理相关数据。
- [x] 实现安全验证处理，避免风险。
- [x] 增加接入人员信息记录
- [x] 数据库增加接入人员连接记录
- [x] 增加接入人员在线状态管理
- [ ] 链路数据安全加密处理
- [ ] 增加流量统计。
- [ ] 增加黑白名单配置。
- [ ] 增加服务端维护界面。
- [ ] 增加客户端维护节点。
- [ ] 支持docker部署。

# 技术路线
- springboot
- spring data jpa
- netty
- mysql
- lombok


# 特别鸣谢
![JenBrains logo](docs/images/jetbrains.svg)