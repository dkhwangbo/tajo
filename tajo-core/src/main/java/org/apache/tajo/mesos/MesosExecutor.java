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

import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;
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
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Status;

import java.util.ArrayList;
import java.util.List;

public class MesosExecutor implements Executor {

  private static final Log LOG = LogFactory.getLog(MesosExecutor.class);

  @Override
  public void registered(ExecutorDriver executorDriver,
                         ExecutorInfo executorInfo,
                         FrameworkInfo frameworkInfo,
                         SlaveInfo slaveInfo) {

    String executorId = executorInfo.getExecutorId().getValue();
    LOG.info("Registered with Mesos as executor ID " + executorId);
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

        LOG.info("Running task " + taskInfo.getTaskId().getValue());

        // This is where one would perform the requested task.

        startWorker(taskInfo);

      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }}.start();
  }

  private void startWorker(TaskInfo taskInfo) {
    String workerStartCmd = "bin/tajo-daemon.sh start worker";
    List<String> resourceArgs = new ArrayList<>();
    for (Resource resource : taskInfo.getResourcesList()) {
      if (resource.getName().equals("cpus")) {
        resourceArgs.add("--cpus:" + Integer.toString((int) resource.getScalar().getValue()));
      } else if (resource.getName().equals("mem")) {
        resourceArgs.add("--mem:" + Integer.toString((int) resource.getScalar().getValue()));
      }
    }

    String cmd = String.join(" ", workerStartCmd, String.join(" ", resourceArgs));

    runProcess(cmd);
  }

  private void stopWorker() {
    String workerStopCmd = "bin/tajo-daemon.sh stop worker";

    try {
      runProcess(workerStopCmd);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  private void runProcess(String command) {
    Process pro;
    try {
      pro = Runtime.getRuntime().exec(command);
      pro.waitFor();
    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void killTask(ExecutorDriver executorDriver, Protos.TaskID taskID) {
    stopWorker();
    TaskStatus status = TaskStatus.newBuilder()
      .setTaskId(taskID)
      .setState(TaskState.TASK_KILLED).build();

    executorDriver.sendStatusUpdate(status);
  }

  @Override
  public void frameworkMessage(ExecutorDriver executorDriver, byte[] bytes) {

  }

  @Override
  public void shutdown(ExecutorDriver executorDriver) {
    stopWorker();
  }

  @Override
  public void error(ExecutorDriver executorDriver, String s) {
    LOG.error("Error from Mesos: " + s);
  }

  public static void main(String[] args) {
    MesosExecutorDriver driver = new MesosExecutorDriver(new MesosExecutor());
    System.exit(driver.run() == Status.DRIVER_STOPPED ? 0 : 1);
  }
}
