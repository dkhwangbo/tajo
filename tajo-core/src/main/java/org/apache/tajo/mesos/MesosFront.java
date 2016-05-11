/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tajo.mesos;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Scheduler;
import org.apache.tajo.conf.TajoConf;

import java.io.IOException;
import java.lang.reflect.Field;

public class MesosFront {

  public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, InterruptedException, IOException {
    System.setProperty("java.library.path", "/usr/local/lib/");
    Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
    fieldSysPath.setAccessible(true);
    fieldSysPath.set(null, null);

    String uri = "hdfs://localhost:9000/tajo-0.12.0-SNAPSHOT.tar.gz";
    String executorCommand = "cd tajo-0.12*; bin/tajo org.apache.tajo.mesos.MesosExecutor";

    ExecutorInfo executor = ExecutorInfo.newBuilder()
      .setExecutorId(ExecutorID.newBuilder().setValue("default"))
      .setCommand(CommandInfo.newBuilder().addUris(CommandInfo.URI.newBuilder().setValue(uri.trim()))
        .setValue(executorCommand))
      .setName("Test Executor").setSource("test").build();
    FrameworkInfo.Builder frameworkBuilder = FrameworkInfo.newBuilder().setUser("dk").setName("Test Framework");

    Scheduler scheduler = new MesosScheduler(executor);

    String masterUri = "127.0.0.1:5050";

    MesosSchedulerDriver driver = new MesosSchedulerDriver(scheduler, frameworkBuilder.build(), masterUri);
    int status = driver.run() == Status.DRIVER_STOPPED ? 0 : 1;
    driver.stop();
    Thread.sleep(500);
    System.exit(status);
  }
}
