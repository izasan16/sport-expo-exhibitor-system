import { Result } from 'antd';
import { useEffect, useState } from 'react';

function isMobileWidth() {
  return window.innerWidth < 768;
}

export default function MobileUnsupported({ children }: { children: React.ReactNode }) {
  const [mobile, setMobile] = useState(isMobileWidth);

  useEffect(() => {
    const onResize = () => setMobile(isMobileWidth());
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  if (mobile) {
    return (
      <Result
        status="info"
        title="请在电脑端访问展商系统"
        subTitle="当前版本面向展会运营和展商选位工作台，暂不支持手机端操作。"
      />
    );
  }

  return <>{children}</>;
}
