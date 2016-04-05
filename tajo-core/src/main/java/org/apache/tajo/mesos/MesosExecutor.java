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

import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.SlaveInfo;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.Status;

public class MesosExecutor implements Executor {

  @Override
  public void registered(ExecutorDriver executorDriver,
                         ExecutorInfo executorInfo,
                         FrameworkInfo frameworkInfo,
                         SlaveInfo slaveInfo) {

    System.out.println("Registred executor on " + slaveInfo.getHostname());
  }

  @Override
  public void reregistered(ExecutorDriver executorDriver, SlaveInfo slaveInfo) {

  }

  @Override
  public void disconnected(ExecutorDriver executorDriver) {

  }

  @Override
  public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {
    new Thread() { public void run() {
      try {
        TaskStatus status = TaskStatus.newBuilder()
          .setTaskId(taskInfo.getTaskId())
          .setState(TaskState.TASK_RUNNING).build();

        executorDriver.sendStatusUpdate(status);

        System.out.println("Running task " + taskInfo.getTaskId().getValue());

        // This is where one would perform the requested task.


        status = TaskStatus.newBuilder()
          .setTaskId(taskInfo.getTaskId())
          .setState(TaskState.TASK_FINISHED).build();

        executorDriver.sendStatusUpdate(status);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }}.start();
  }

  @Override
  public void killTask(ExecutorDriver executorDriver, Protos.TaskID taskID) {

  }

  @Override
  public void frameworkMessage(ExecutorDriver executorDriver, byte[] bytes) {

  }

  @Override
  public void shutdown(ExecutorDriver executorDriver) {

  }

  @Override
  public void error(ExecutorDriver executorDriver, String s) {

  }

  public static void main(String[] args) {
    MesosExecutorDriver driver = new MesosExecutorDriver(new MesosExecutor());
    System.exit(driver.run() == Status.DRIVER_STOPPED ? 0 : 1);
  }
}
