/*
 * 팀원이 만든 '시나리오별 절약 제안' 데이터·엔진 (3.agent-proposals)을 그대로 가져와 씁니다.
 * 원본 파일은 globalThis 에 값을 올리는 스크립트라, 아래 순서대로 불러와야 해요.
 *   scenarios → scenario-engine
 */
import './data/scenarios.js';
import './scenario-engine.js';

export const engine = globalThis.WonnieDemoEngine;
