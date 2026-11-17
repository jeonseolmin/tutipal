import { useState } from 'react'

/** 방 만들기 · 주제 설정에서 고르는 과목. 코딩 · 영어 · 수학은 서버에 전용 챗봇 지시문이 있다 (TopicPrompt) */
export const SUBJECTS = [
  { group: '학교 과목', items: ['국어', '영어', '수학', '과학', '사회', '한국사', '물리', '화학', '생명과학', '지구과학'] },
  { group: 'IT', items: ['코딩', '알고리즘', '웹 개발', '데이터 · AI', '정보보안', '디자인'] },
  { group: '외국어', items: ['일본어', '중국어', '스페인어', '프랑스어', '독일어'] },
  { group: '시험 · 자격증', items: ['수능', '토익', '토플', '공무원 시험', '정보처리기사', '자격증'] },
  { group: '그 밖에', items: ['경제 · 경영', '음악', '미술', '자유 주제'] },
]
const ALL = SUBJECTS.flatMap((g) => g.items)
const CUSTOM = '__custom__'
/** CreateSessionRequest.topic 의 @Size(max) 와 같게 */
export const SUBJECT_MAX = 30

/** 드롭다운에서 고르거나 "직접 입력" 을 골라 이름을 쓴다. value 는 과목 이름 문자열 */
export default function SubjectPicker({ value, onChange, disabled }) {
  const [custom, setCustom] = useState(() => !!value && !ALL.includes(value))
  return (
    <div className="subject-picker">
      <select
        value={custom ? CUSTOM : value}
        onChange={(e) => {
          const v = e.target.value
          setCustom(v === CUSTOM)
          onChange(v === CUSTOM ? '' : v)
        }}
        disabled={disabled}
        aria-label="공부할 과목"
      >
        <option value="" disabled>과목을 골라 주세요</option>
        {SUBJECTS.map((g) => (
          <optgroup key={g.group} label={g.group}>
            {g.items.map((s) => <option key={s} value={s}>{s}</option>)}
          </optgroup>
        ))}
        <option value={CUSTOM}>직접 입력</option>
      </select>
      {custom && (
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          maxLength={SUBJECT_MAX}
          placeholder="예: 드론 조종, 바리스타 자격증"
          disabled={disabled}
          aria-label="과목 이름 직접 입력"
          autoFocus
        />
      )}
    </div>
  )
}
