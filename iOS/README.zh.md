# 秒切/秒开场景化API          

*[English](README.md) | 中文*

本文档主要介绍如何快速集成秒切/秒开场景化 API

## 1.环境准备
- Xcode 13.0及以上版本
- 最低支持系统：iOS 12.0
- 请确保您的项目已设置有效的开发者签名
  
## 2.运行示例
- 克隆或者直接下载项目源码
- 获取声网App ID -------- [声网Agora - 文档中心 - 如何获取 App ID](https://docs.agora.io/cn/Agora%20Platform/get_appid_token?platform=All%20Platforms#%E8%8E%B7%E5%8F%96-app-id)
  
  > - 点击创建应用
  >   
  >   ![](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/create_app_1.jpg)
  > 
  > - 选择你要创建的应用类型
  >   
  >   ![](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/create_app_2.jpg)
  > 

- 获取App 证书 ----- [声网Agora - 文档中心 - 获取 App 证书](https://docs.agora.io/cn/Agora%20Platform/get_appid_token?platform=All%20Platforms#%E8%8E%B7%E5%8F%96-app-%E8%AF%81%E4%B9%A6)
  
  > 在声网控制台的项目管理页面，找到你的项目，点击配置。
  > ![](https://fullapp.oss-cn-beijing.aliyuncs.com/scenario_api/callapi/config/1641871111769.png)
  > 点击主要证书下面的复制图标，即可获取项目的 App 证书。
  > ![](https://fullapp.oss-cn-beijing.aliyuncs.com/scenario_api/callapi/config/1637637672988.png)
- 秒切机器人服务配置（CloudPlayer）
    ```json
    注: 请联系声网技术支持为您的 APPID 开通 rte-cloudplayer 权限, 开通权限后才能启动默认的机器人房间推流
    ```
    
    ![图片](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/ent-full/sdhy_4.jpg)
    ![图片](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/ent-full/sdhy_5.jpg)
    ![图片](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/ent-full/cloud_player_key_ios.jpg)
- 在项目的[KeyCenter.swift](Example/VideoLoaderAPI/KeyCenter.swift) 中填入声网的AppId、Certificate、机器人推流配置(CloudPlayerKey、CloudPlayerSecret)
  
  ```
  static var AppId: String = <#Your AppId#>
  static var Certificate: String = <#Your Certificate#>
  static let CloudPlayerKey: String? = <#Your CloudPlayerKey#>
  static let CloudPlayerSecret: String? = <#Your CloudPlayerSecret#>
  ```
- 打开终端，进入到[Podfile](Example/Podfile)目录下，执行`pod install`命令，生成`VideoLoaderAPI.xcworkspace`文件
- 最后打开`VideoLoaderAPI.xcworkspace`，运行即可开始您的体验

## 3. 项目介绍

- <mark>1. 概述</mark>
> VideoLoaderAPI 即秒开秒切场景化api, 该模块旨在帮助视频直播开发者更快集成声网秒切、秒开相关能力的最佳实践
>
- <mark>2. 功能介绍</mark>
> VideoLoaderAPI Demo 目前已涵盖以下功能
> - 选择预加载模式和视频出图模式
>
>   相关代码请参考：[DebugSettingViewController.swift](Example/VideoLoaderAPI/DebugSettingViewController.swift)
>
> - 秒开
>
>   相关代码请参考：[RoomCollectionListViewController.swift](Example/VideoLoaderAPI/RoomCollectionListViewController.swift)
>
> - 秒切
> 
>     相关代码请参考：[RoomCollectionViewController.swift](Example/VideoLoaderAPI/Normal/CollectionView/CollectionRoomViewController.swift) 
>
- 3.文件简介

相关核心代码请参考：[VideoLoaderAPI](VideoLoaderAPI/Classes/)

* [UIView+VideoLoader.swift](VideoLoaderAPI/Classes/UI/UIView+VideoLoader.swift): 秒开事件处理模块
* [AGCollectionLoadingDelegateHandler.swift](VideoLoaderAPI/Classes/UI/AGCollectionLoadingDelegateHandler.swift): 房间列表滑动事件处理模块
* [AGCollectionSlicingDelegateHandler.swift](VideoLoaderAPI/Classes/UI/AGCollectionSlicingDelegateHandler.swift): 直播间切换事件处理模块
* [VideoLoaderApiImpl.swift](VideoLoaderAPI/Classes/VideoLoaderApiImpl.swift): 内部使用处理频道管理类

## 4.快速集成
请参考官网文档 [集成 VideoLoaderAPI](https://doc.shengwang.cn/doc/showroom/ios/advanced-features/video-loader/integrate)
