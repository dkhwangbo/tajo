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

import org.apache.mesos.Protos.*;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

import java.util.*;

public class MesosScheduler implements Scheduler {

  private final ExecutorInfo executor;
  private final int totalTasks;
  private int launchedTasks = 0;
  private int finishedTasks = 0;

  public MesosScheduler(ExecutorInfo executor) {
    this(executor, 1);
  }

  public MesosScheduler(ExecutorInfo executor,
                        int totalTasks) {
    this.executor = executor;
    this.totalTasks = totalTasks;
  }

  @Override
  public void registered(SchedulerDriver driver,
                         FrameworkID frameworkId,
                         MasterInfo masterInfo) {
    System.out.println("Registered! ID = " + frameworkId.getValue());
  }

  @Override
  public void reregistered(SchedulerDriver driver, MasterInfo masterInfo) {
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
  }

  @Override
  public void resourceOffers(SchedulerDriver driver,
                             List<Offer> offers) {
    double CPUS_PER_TASK = 2;
    double MEM_PER_TASK = 2048;

    for (Offer offer : offers) {
      Offer.Operation.Launch.Builder launch = Offer.Operation.Launch.newBuilder();
      double offerCpus = 0;
      double offerMem = 0;
      for (Resource resource : offer.getResourcesList()) {
        if (resource.getName().equals("cpus")) {
          offerCpus += resource.getScalar().getValue();
        } else if (resource.getName().equals("mem")) {
          offerMem += resource.getScalar().getValue();
        }
      }

      System.out.println(
        "Received offer " + offer.getId().getValue() + " with cpus: " + offerCpus +
          " and mem: " + offerMem);

      double remainingCpus = offerCpus;
      double remainingMem = offerMem;
      while (launchedTasks < totalTasks &&
        remainingCpus >= CPUS_PER_TASK &&
        remainingMem >= MEM_PER_TASK) {
        TaskID taskId = TaskID.newBuilder()
          .setValue(Integer.toString(launchedTasks++)).build();

        System.out.println("Launching task " + taskId.getValue() +
          " using offer " + offer.getId().getValue());

        TaskInfo task = TaskInfo.newBuilder()
          .setName("task " + taskId.getValue())
          .setTaskId(taskId)
          .setSlaveId(offer.getSlaveId())
          .addResources(Resource.newBuilder()
            .setName("cpus")
            .setType(Value.Type.SCALAR)
            .setScalar(Value.Scalar.newBuilder().setValue(CPUS_PER_TASK)))
          .addResources(Resource.newBuilder()
            .setName("mem")
            .setType(Value.Type.SCALAR)
            .setScalar(Value.Scalar.newBuilder().setValue(MEM_PER_TASK)))
          .setExecutor(ExecutorInfo.newBuilder(executor))
          .build();

        launch.addTaskInfos(TaskInfo.newBuilder(task));

        remainingCpus -= CPUS_PER_TASK;
        remainingMem -= MEM_PER_TASK;
      }

      // NOTE: We use the new API `acceptOffers` here to launch tasks. The
      // 'launchTasks' API will be deprecated.
      List<OfferID> offerIds = new ArrayList<>();
      offerIds.add(offer.getId());

      List<Offer.Operation> operations = new ArrayList<>();

      Offer.Operation operation = Offer.Operation.newBuilder()
        .setType(Offer.Operation.Type.LAUNCH)
        .setLaunch(launch)
        .build();

      operations.add(operation);

      Filters filters = Filters.newBuilder().setRefuseSeconds(1).build();

      driver.acceptOffers(offerIds, operations, filters);
    }
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, OfferID offerId) {
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, TaskStatus status) {
    System.out.println("Status update: task " + status.getTaskId().getValue() +
      " is in state " + status.getState().getValueDescriptor().getName());
    if (status.getState() == TaskState.TASK_FINISHED) {
      finishedTasks++;
      System.out.println("Finished tasks: " + finishedTasks);
      if (finishedTasks == totalTasks) {
        driver.stop();
      }
    }

    if (status.getState() == TaskState.TASK_LOST ||
      status.getState() == TaskState.TASK_KILLED ||
      status.getState() == TaskState.TASK_FAILED) {
      System.err.println("Aborting because task " + status.getTaskId().getValue() +
        " is in unexpected state " +
        status.getState().getValueDescriptor().getName() +
        " with reason '" +
        status.getReason().getValueDescriptor().getName() + "'" +
        " from source '" +
        status.getSource().getValueDescriptor().getName() + "'" +
        " with message '" + status.getMessage() + "'");
      driver.abort();
    }

  }

  @Override
  public void frameworkMessage(SchedulerDriver driver,
                               ExecutorID executorId,
                               SlaveID slaveId,
                               byte[] data) {
  }

  @Override
  public void slaveLost(SchedulerDriver driver, SlaveID slaveId) {
    System.err.println("slaveLost is called! slaveID :" + slaveId);
  }

  @Override
  public void executorLost(SchedulerDriver driver,
                           ExecutorID executorId,
                           SlaveID slaveId,
                           int status) {
    System.err.println("executorLost is called!");
  }

  public void error(SchedulerDriver driver, String message) {
    System.err.println("Error: " + message);
  }

}
