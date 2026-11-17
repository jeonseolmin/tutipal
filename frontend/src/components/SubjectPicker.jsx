import { useEffect, useState } from 'react'

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

/**
 * 드롭다운에서 고르거나 "직접 입력" 을 골라 이름을 쓴다. value 는 과목 이름 문자열.
 * lazy: 직접 입력한 이름을 다 쓴 뒤(Enter · 칸 밖으로)에만 onChange — 바뀔 때마다 대화를 새로 여는 챗봇용
 */
export default function SubjectPicker({ value, onChange, disabled, lazy = false }) {
  // 목록에 없는 이름이면 직접 입력. picked = 방금 "직접 입력" 을 골랐고 아직 이름이 정해지지 않음
  const [picked, setPicked] = useState(false)
  const custom = picked || (!!value && !ALL.includes(value))
  const [draft, setDraft] = useState(value)
  // 밖에서 값이 바뀌면(이름 확정 · 다른 화면에서 바꿈) 그 값을 따른다
  useEffect(() => {
    setDraft(value)
    if (value) setPicked(false)
  }, [value])
  const commit = () => { if (draft.trim() && draft !== value) onChange(draft) }
  return (
    <div className="subject-picker">
      <select
        value={custom ? CUSTOM : value}
        onChange={(e) => {
          const v = e.target.value
          setPicked(v === CUSTOM)
          if (v !== CUSTOM) onChange(v)
          else if (lazy) setDraft('')        // 이름을 다 쓸 때까지 지금 과목 유지
          else onChange('')
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
          value={lazy ? draft : value}
          onChange={(e) => (lazy ? setDraft(e.target.value) : onChange(e.target.value))}
          onBlur={lazy ? commit : undefined}
          onKeyDown={lazy ? (e) => { if (e.key === 'Enter') { e.preventDefault(); commit() } } : undefined}
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
