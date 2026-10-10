/** Irene's paper-circle motif. Decorative; meaningful session data lives in text. */
export default function CircleBloom({ compact = false }) {
  return <div className={`circle-bloom${compact ? ' compact' : ''}`} aria-hidden="true">
    <div className="bloom-orbit orbit-one"/><div className="bloom-orbit orbit-two"/>
    <div className="bloom-disc disc-one"/><div className="bloom-disc disc-two"/>
    <div className="bloom-disc disc-three"/><div className="bloom-disc disc-four"/>
    <div className="bloom-center"><span>room</span><i>for you.</i></div>
    <span className="bloom-spark spark-one">✳</span><span className="bloom-spark spark-two">+</span>
    {!compact && <><span className="bloom-note">a shared beginning</span><span className="bloom-caption">A SMALL CIRCLE. A LITTLE MORE POSSIBILITY.</span></>}
  </div>;
}
