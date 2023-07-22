package cn.ChengZhiYa.BaiShenLauncher.ui.wizard;

public enum WizardNavigationResult {
    PROCEED {
        @Override
        public boolean getDeferredComputation() {
            return true;
        }
    },
    DENY {
        @Override
        public boolean getDeferredComputation() {
            return false;
        }
    };

    public abstract boolean getDeferredComputation();
}
