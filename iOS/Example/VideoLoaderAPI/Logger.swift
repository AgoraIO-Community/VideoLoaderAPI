//
//  Logger.swift
//  VideoLoaderAPI_Example
//
//  Created by wushengtao on 2024/4/24.
//  Copyright © 2024 CocoaPods. All rights reserved.
//

import SwiftyBeaver

func createLog() -> SwiftyBeaver.Type {
    let log = SwiftyBeaver.self
    
    // add log destinations. at least one is needed!
    let console = ConsoleDestination()
     // log to Xcode Console
    let file = FileDestination()  // log to default swiftybeaver.log file
    let logDir = logsDir()
    file.logFileURL = URL(fileURLWithPath: "\(logDir)/agora_ent_videoloader.log")
    
    // use custom format and set console output to short time, log level & message
    console.format = "[Demo][$L][$X]$Dyyyy-MM-dd HH:mm:ss.SSS$d $M"
    file.format = console.format
    file.logFileMaxSize = 1 * 1024 * 1024
    file.logFileAmount = 2
    // or use this for JSON output: console.format = "$J"

    // add the destinations to SwiftyBeaver
    #if DEBUG
    log.addDestination(console)
    #endif
    log.addDestination(file)

    return log
}

func cacheDir() ->String {
    let dir = NSSearchPathForDirectoriesInDomains(FileManager.SearchPathDirectory.cachesDirectory,
                                                  FileManager.SearchPathDomainMask.userDomainMask, true).first
    return dir ?? ""
}

func logsDir() ->String {
    let dir = cacheDir()
    let logDir = "\(dir)/ExampleLogs"
    try? FileManager.default.createDirectory(at: URL(fileURLWithPath: logDir), withIntermediateDirectories: true)
    
    return logDir
}

let logger = createLog()
public func agora_info(_ text: String, tag: String = "Example") {
    logger.info(text, context: tag)
}

public func agora_warn(_ text: String, tag: String = "Example") {
    logger.warning(text, context: tag)
}

public func agora_error(_ text: String, tag: String = "Example") {
    logger.error(text, context: tag)
}
