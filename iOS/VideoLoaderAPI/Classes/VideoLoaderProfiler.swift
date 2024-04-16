//
//  VideoLoaderProfiler.swift
//  VideoLoaderAPI
//
//  Created by wushengtao on 2023/8/14.
//

import Foundation
import AgoraRtcKit


public func debugLoaderPrint(_ message: String) {
    if let engine = VideoLoaderApiImpl.shared.config?.rtcEngine {
        engine.writeLog(.info, content: "[VideoLoaderApi]\(message)")
    }
    if let closure = VideoLoaderApiImpl.shared.printClosure {
        closure(message)
        return
    }
    debugApiPrint(message)
}

public func warningLoaderPrint(_ message: String) {
    if let engine = VideoLoaderApiImpl.shared.config?.rtcEngine {
        engine.writeLog(.warn, content: "[VideoLoaderApi]\(message)")
    }
    if let closure = VideoLoaderApiImpl.shared.warningClosure {
        closure(message)
        return
    }
    debugApiPrint("[Warning]\(message)")
    
}

public func errorLoaderPrint(_ message: String) {
    if let engine = VideoLoaderApiImpl.shared.config?.rtcEngine {
        engine.writeLog(.error, content: "[VideoLoaderApi]\(message)")
    }
    if let closure = VideoLoaderApiImpl.shared.errorClosure {
        closure(message)
        return
    }
    debugApiPrint("[Error]\(message)")
}

class VideoLoaderProfiler: NSObject, AgoraRtcEngineDelegate {
    var anchorId: String!
    public internal(set) var actualStartTime: Int64 = 0
    public internal(set) var perceivedStartTime: Int64 = 0
    public var reportExt: [String: Any] = [:]
    var firstFrameCompletion: ((Int64, UInt)->())?
    
    init(anchorId: String) {
        self.anchorId = anchorId
    }
    public func rtcEngine(_ engine: AgoraRtcEngineKit,
                          remoteVideoStateChangedOfUid uid: UInt,
                          state: AgoraVideoRemoteState,
                          reason: AgoraVideoRemoteReason,
                          elapsed: Int) {
        let currentTs = Int64(Date().timeIntervalSince1970 * 1000)
        let actualCost = currentTs - actualStartTime
        let perceivedCost = currentTs - perceivedStartTime
        let anchorId = anchorId ?? ""
        #if DEBUG
        debugLoaderPrint("remoteVideoStateChangedOfUid[\(anchorId)]: \(uid) state: \(state.rawValue) reason: \(reason.rawValue)")
        #endif
        DispatchQueue.main.async {
            if state == .decoding /*2*/,
               ( reason == .remoteUnmuted /*6*/ || reason == .localUnmuted /*4*/ || reason == .localMuted /*3*/ )   {
                debugLoaderPrint("anchorId[\(anchorId)] uid[\(uid)] show first frame! actualCost: \(actualCost) ms perceivedCost: \(perceivedCost) ms")
                var ext = self.reportExt
                ext["channelName"] = anchorId
                VideoLoaderApiImpl.shared.reporter?.reportCostEvent(name: APICostEvent.firstFrameActual, cost: Int(actualCost), ext: ext)
                if self.perceivedStartTime > 0 {
                    VideoLoaderApiImpl.shared.reporter?.reportCostEvent(name: APICostEvent.firstFramePerceived, cost: Int(perceivedCost), ext: ext)
                }
                self.firstFrameCompletion?(actualCost, uid)
            }
        }
    }
}
