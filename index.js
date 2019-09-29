import {Platform, NativeModules, DeviceEventEmitter, NativeAppEventEmitter} from 'react-native';
const { Uapp } = NativeModules, messageEvent = 'UmengMessage', IsAndroid = Platform.OS === 'android';

// 统计打点
Uapp.onEvent = (eventId, data, duration) => {
  const dt = typeof data;
  if (dt === 'undefined') {
    return Uapp.onEventTrick(eventId)
  }
  if (dt === 'string') {
    const temp = {};
    temp[eventId] = data;
    data = temp;
  }
  if (typeof duration === 'undefined') {
    return Uapp.onEventObject(eventId, data)
  }
  return Uapp.onEventDuration(eventId, data, duration)
}

// 监听推送消息
const emitMessage = (listener, msg) => {
  ['body', 'extra'].forEach(key => {
    if (!msg.hasOwnProperty(key)) {
      return;
    }
    try {
      msg[key] = JSON.parse(msg[key])
    }catch(e) {
      // do nothing
    }
  });
  listener(msg)
}
Uapp.onMessage = (listener) => {
    if(IsAndroid) {
      DeviceEventEmitter.addListener(messageEvent, msg => {
        emitMessage(listener, msg);
      });
    } else {
      NativeAppEventEmitter.addListener(messageEvent, msg => {
        emitMessage(listener, msg);
      });
    }
}

export default Uapp;
