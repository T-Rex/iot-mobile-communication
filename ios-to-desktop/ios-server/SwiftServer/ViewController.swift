//
//  ViewController.swift
//  SwiftServer
//
//  Created by Denis on 10/31/16.
//  Copyright © 2016 it-dimension. All rights reserved.
//

import UIKit
import Foundation
import Darwin.C

class ViewController: UIViewController {

    @IBOutlet var logView : UITextView!
    @IBOutlet var firstField : UITextField!
    @IBOutlet var secondField : UITextField!

    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    @IBAction func onConnect (sender: UIButton){
        sender.removeFromSuperview()
        
        let backgroundQueue = DispatchQueue.global()
        backgroundQueue.async {
            self.startTCPServer()
        }
    }

    func startTCPServer(){
        let server:TCPServer = TCPServer(addr: "127.0.0.1", port: 8080)
        let (success,msg)=server.listen()
        if success{
            while true{
                if let client=server.accept(){
                    echoService(client: client)
                }
                else{
                    print("accept error")
                }
            }
        }
        else{
            print(msg)
        }
    }
    
    func echoService(client c:TCPClient){
        DispatchQueue.main.async {
            self.logView.text = "";
            self.logView.text.append("==========================\n");
            self.logView.text.append("newclient from:\(c.addr)[\(c.port)]\n")
        }
        
        let sendString = "add;\(firstField.text!);\(secondField.text!)"
        DispatchQueue.main.async {
            self.logView.text.append("send:\(sendString)\n")
        }
        
        let sendResult = c.send(str: sendString)
        DispatchQueue.main.async {
            self.logView.text.append("\(sendResult.1)\n")
        }
        
        let bytes = c.read(1024, timeout: -1)
        
        if let str = String(bytes: bytes!, encoding: String.Encoding.utf8) {
            var reciveSctring = str;
            reciveSctring = reciveSctring.replacingOccurrences(of: "\n", with: "");
            reciveSctring = reciveSctring.replacingOccurrences(of: "\r", with: "");
            
            DispatchQueue.main.async {
                self.logView.text.append("recive:\(reciveSctring)\n")
            }
        }
        
        let closeResult = c.close()
        
        DispatchQueue.main.async {
            self.logView.text.append("close connection\n")
            self.logView.text.append("\(closeResult.1)\n")
            self.logView.text.append("==========================\n\n");
        }
        
    
    }
    

}

