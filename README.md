# 黑马点评
本部分为根据黑马点评的视频完成的代码。

注意事项：
1、在项目启动时，项目一直报错：NOGROUP No such key ‘stream.orders’ or consumer group ‘g1’ in XREADGROUP with GROUP option
原因：redis中没有stream.orders键
解决方法：创建一个Stream类型的消息队列，名为stream.orders
命令：XGROUP CREATE stream.orders g1 0 MKSTREAM
重新启动项目，就不报错了

