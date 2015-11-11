/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.am;

import android.app.IActivityManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;

import java.io.PrintWriter;

class ActivityManagerShellCommand extends ShellCommand {
    // IPC interface to activity manager -- don't need to do additional security checks.
    final IActivityManager mInterface;

    // Internal service impl -- must perform security checks before touching.
    final ActivityManagerService mInternal;

    final boolean mDumping;

    ActivityManagerShellCommand(ActivityManagerService service, boolean dumping) {
        mInterface = service;
        mInternal = service;
        mDumping = dumping;
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd) {
                case "force-stop":
                    return runForceStop(pw);
                case "kill":
                    return runKill(pw);
                case "kill-all":
                    return runKillAll(pw);
                case "write":
                    return runWrite(pw);
                case "track-associations":
                    return runTrackAssociations(pw);
                case "untrack-associations":
                    return runUntrackAssociations(pw);
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
        }
        return -1;
    }

    int runForceStop(PrintWriter pw) throws RemoteException {
        int userId = UserHandle.USER_ALL;

        String opt;
        while ((opt = getNextOption()) != null) {
            if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                pw.println("Error: Unknown option: " + opt);
                return -1;
            }
        }
        mInterface.forceStopPackage(getNextArgRequired(), userId);
        return 0;
    }

    int runKill(PrintWriter pw) throws RemoteException {
        int userId = UserHandle.USER_ALL;

        String opt;
        while ((opt=getNextOption()) != null) {
            if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                pw.println("Error: Unknown option: " + opt);
                return -1;
            }
        }
        mInterface.killBackgroundProcesses(getNextArgRequired(), userId);
        return 0;
    }

    int runKillAll(PrintWriter pw) throws RemoteException {
        mInterface.killAllBackgroundProcesses();
        return 0;
    }

    int runWrite(PrintWriter pw) {
        mInternal.enforceCallingPermission(android.Manifest.permission.SET_ACTIVITY_WATCHER,
                "registerUidObserver()");
        mInternal.mTaskPersister.flush();
        pw.println("All tasks persisted.");
        return 0;
    }

    int runTrackAssociations(PrintWriter pw) {
        mInternal.enforceCallingPermission(android.Manifest.permission.SET_ACTIVITY_WATCHER,
                "registerUidObserver()");
        synchronized (mInternal) {
            if (!mInternal.mTrackingAssociations) {
                mInternal.mTrackingAssociations = true;
                pw.println("Association tracking started.");
            } else {
                pw.println("Association tracking already enabled.");
            }
        }
        return 0;
    }

    int runUntrackAssociations(PrintWriter pw) {
        mInternal.enforceCallingPermission(android.Manifest.permission.SET_ACTIVITY_WATCHER,
                "registerUidObserver()");
        synchronized (mInternal) {
            if (mInternal.mTrackingAssociations) {
                mInternal.mTrackingAssociations = false;
                mInternal.mAssociations.clear();
                pw.println("Association tracking stopped.");
            } else {
                pw.println("Association tracking not running.");
            }
        }
        return 0;
    }

    @Override
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        dumpHelp(pw, mDumping);
    }

    static void dumpHelp(PrintWriter pw, boolean dumping) {
        if (dumping) {
            pw.println("Activity manager dump options:");
            pw.println("  [-a] [-c] [-p PACKAGE] [-h] [WHAT] ...");
            pw.println("  WHAT may be one of:");
            pw.println("    a[ctivities]: activity stack state");
            pw.println("    r[recents]: recent activities state");
            pw.println("    b[roadcasts] [PACKAGE_NAME] [history [-s]]: broadcast state");
            pw.println("    i[ntents] [PACKAGE_NAME]: pending intent state");
            pw.println("    p[rocesses] [PACKAGE_NAME]: process state");
            pw.println("    o[om]: out of memory management");
            pw.println("    perm[issions]: URI permission grant state");
            pw.println("    prov[iders] [COMP_SPEC ...]: content provider state");
            pw.println("    provider [COMP_SPEC]: provider client-side state");
            pw.println("    s[ervices] [COMP_SPEC ...]: service state");
            pw.println("    as[sociations]: tracked app associations");
            pw.println("    service [COMP_SPEC]: service client-side state");
            pw.println("    package [PACKAGE_NAME]: all state related to given package");
            pw.println("    all: dump all activities");
            pw.println("    top: dump the top activity");
            pw.println("  WHAT may also be a COMP_SPEC to dump activities.");
            pw.println("  COMP_SPEC may be a component name (com.foo/.myApp),");
            pw.println("    a partial substring in a component name, a");
            pw.println("    hex object identifier.");
            pw.println("  -a: include all available server state.");
            pw.println("  -c: include client state.");
            pw.println("  -p: limit output to given package.");
        } else {
            pw.println("Activity manager (activity) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  force-stop [--user <USER_ID> | all | current] <PACKAGE>");
            pw.println("    Complete stop the given application package.");
            pw.println("  kill [--user <USER_ID> | all | current] <PACKAGE>");
            pw.println("    Kill all processes associated with the given application.");
            pw.println("  kill-all");
            pw.println("    Kill all processes that are safe to kill (cached, etc)");
            pw.println("  write");
            pw.println("    Write all pending state to storage.");
            pw.println("  track-associations");
            pw.println("    Enable association tracking.");
            pw.println("  untrack-associations");
            pw.println("    Disable and clear association tracking.");
        }
    }
}
