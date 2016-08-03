Simple task manager on akka persistence
=======================================

Just naive implementation of task manager on akka persistence

## Usage
Run http server
```
sbt re-start
```

Run task after 10 seconds
```
curl -H "Content-Type: application/json" -X POST -d '{"time":10}' http://localhost:9000/task
```

## Demo

```
> re-start
[info] Application root not yet started
[info] Starting application root in the background ...
root Starting io.github.maxkorolev.base.Main.main()
[success] Total time: 1 s, completed 03.08.2016 16:43:12
> root [INFO] [08/03/2016 16:43:14.765] [default-akka.actor.default-dispatcher-2] [akka://default/user/task_manager] RecoveryCompleted TaskManagerState(Map(),None,None)
root [INFO] [08/03/2016 16:43:22.001] [default-akka.actor.default-dispatcher-4] [app(akka://default)] /task executed
root [INFO] [08/03/2016 16:43:22.052] [default-akka.actor.default-dispatcher-3] [akka://default/user/task_manager] TaskPushed TaskManagerState(Map(1470231812034 -> Task(time: 1470231812034, timeout: 10 seconds)),None,None)
root [INFO] [08/03/2016 16:43:22.109] [default-akka.actor.default-dispatcher-10] [akka://default/user/task_manager/task-1470231812034] Task will be executed in a 2016-08-03 16:43:32
root [INFO] [08/03/2016 16:43:32.062] [default-akka.actor.default-dispatcher-3] [akka://default/user/task_manager/task-1470231812034] Task will be executed now
root [INFO] [08/03/2016 16:43:32.064] [default-akka.actor.default-dispatcher-10] [akka://default/user/task_manager] TaskPulled TaskManagerState(Map(),Some(Actor[akka://default/user/task_manager/task-1470231812034#1861729955]),None)
root [INFO] [08/03/2016 16:43:35.076] [default-akka.actor.default-dispatcher-10] [akka://default/user/task_manager/task-1470231812034] Task has been finished successful
root [INFO] [08/03/2016 16:43:35.079] [default-akka.actor.default-dispatcher-2] [akka://default/user/task_manager] StopRunning TaskManagerState(Map(),None,None)
```
