self.addEventListener("push",event=>{
  let data={};
  try{data=event.data?event.data.json():{}}catch(e){data={body:event.data?.text()||""}}
  const title=data.title||"GAP – Aufgaben-Erinnerung";
  const options={
    body:data.body||"Es gibt noch offene Aufgaben.",
    icon:"favicon.svg",
    badge:"favicon.svg",
    tag:data.tag||"gap-task-reminder",
    renotify:true,
    data:{url:data.url||"/"}
  };
  event.waitUntil(self.registration.showNotification(title,options));
});
self.addEventListener("notificationclick",event=>{
  event.notification.close();
  const url=event.notification.data?.url||"/";
  event.waitUntil(clients.matchAll({type:"window",includeUncontrolled:true}).then(list=>{
    for(const client of list){if("focus" in client){client.navigate(url);return client.focus()}}
    return clients.openWindow(url);
  }));
});